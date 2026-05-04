package com.ticketing.orderservice.service;

import com.ticketing.orderservice.client.EventServiceClient;
import com.ticketing.orderservice.dto.*;
import com.ticketing.orderservice.entity.Order;
import com.ticketing.orderservice.entity.OrderItem;
import com.ticketing.orderservice.entity.OrderStatus;
import com.ticketing.orderservice.exception.*;
import com.ticketing.orderservice.repository.OrderRepository;
import com.ticketing.orderservice.repository.TicketTierRepository;
import com.ticketing.orderservice.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final EventServiceClient eventServiceClient;
    private final TicketTierRepository ticketTierRepository;
    private final WalletRepository walletRepository;
    private final AuditService auditService;

    @Value("${mock.payment.checkout:false}")
    private boolean mockPaymentCheckout;

    public OrderService(OrderRepository orderRepository,
                        EventServiceClient eventServiceClient,
                        TicketTierRepository ticketTierRepository,
                        WalletRepository walletRepository,
                        AuditService auditService) {
        this.orderRepository = orderRepository;
        this.eventServiceClient = eventServiceClient;
        this.ticketTierRepository = ticketTierRepository;
        this.walletRepository = walletRepository;
        this.auditService = auditService;
    }

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request, Long buyerId) {
        EventServiceResponse event = eventServiceClient.getEvent(request.getEventId())
                .orElseThrow(() -> new EventNotFoundException(request.getEventId()));

        if (!"PUBLISHED".equals(event.getStatus())) {
            throw new InvalidEventStatusException(event.getStatus());
        }

        Instant now = Instant.now();
        BigDecimal totalAmount = BigDecimal.ZERO;

        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        List<OrderItemResponse> responseItems = new ArrayList<>();

        for (OrderItemRequest itemRequest : request.getItems()) {
            EventServiceResponse.TierResponse tier = event.getTiers().stream()
                    .filter(t -> t.getId().equals(itemRequest.getTierId()))
                    .findFirst()
                    .orElseThrow(() -> new TierNotFoundException(itemRequest.getTierId()));

            if (!"ACTIVE".equals(tier.getStatus())) {
                throw new InvalidTierStatusException(tier.getStatus());
            }

            if (itemRequest.getQuantity() > tier.getMaxPerOrder()) {
                throw new QuantityExceedsMaxPerOrderException(itemRequest.getQuantity(), tier.getMaxPerOrder());
            }

            if (tier.getRemainingQty() < itemRequest.getQuantity()) {
                throw new InsufficientInventoryException(itemRequest.getQuantity(), tier.getRemainingQty());
            }

            BigDecimal itemTotal = tier.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setTierId(tier.getId());
            orderItem.setTierName(tier.getName());
            orderItem.setEventTitle(event.getTitle());
            Instant eventDate = LocalDateTime.parse(event.getEventDate()).toInstant(ZoneOffset.UTC);
            orderItem.setEventDate(eventDate);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(tier.getPrice());
            orderItem.setVenueName(event.getVenue() != null ? event.getVenue().getName() : null);
            orderItem.setCreatedAt(now);
            order.addItem(orderItem);

            responseItems.add(new OrderItemResponse(tier.getId(), itemRequest.getQuantity(), tier.getPrice()));
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        auditService.logOrderCreated(savedOrder.getId(), buyerId, request.getItems().size(), totalAmount.toString());

        if (mockPaymentCheckout) {
            int debited = walletRepository.debitWallet(buyerId, totalAmount);
            if (debited == 0) {
                throw new InsufficientWalletBalanceException(totalAmount);
            }
            for (OrderItem item : savedOrder.getItems()) {
                int decremented = ticketTierRepository.decrementRemainingQty(item.getTierId(), item.getQuantity());
                if (decremented == 0) {
                    throw new InsufficientInventoryException(item.getQuantity(), 0);
                }
            }
            savedOrder.setStatus(OrderStatus.CONFIRMED);
            savedOrder.setUpdatedAt(Instant.now());
            orderRepository.save(savedOrder);
            auditService.logOrderConfirmed(savedOrder.getId());
            BigDecimal remainingBalance = walletRepository.getBalance(buyerId);
            return new CreateOrderResponse(savedOrder.getId(), OrderStatus.CONFIRMED.name(), totalAmount, responseItems, remainingBalance);
        }

        return new CreateOrderResponse(savedOrder.getId(), OrderStatus.PENDING.name(), totalAmount, responseItems);
    }

    @Transactional
    public CancelOrderResponse cancelOrder(Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.getBuyerId().equals(buyerId)) {
            throw new OrderAccessDeniedException(orderId);
        }

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new OrderCancellationNotAllowedException(orderId,
                    "only CONFIRMED orders can be cancelled");
        }

        Instant eventDate = order.getItems().stream()
                .map(OrderItem::getEventDate)
                .findFirst()
                .orElseThrow(() -> new OrderCancellationNotAllowedException(orderId, "no items found"));

        if (Instant.now().isAfter(eventDate.minus(24, ChronoUnit.HOURS))) {
            throw new OrderCancellationNotAllowedException(orderId,
                    "cancellation is not allowed within 24 hours of the event");
        }

        for (OrderItem item : order.getItems()) {
            ticketTierRepository.incrementRemainingQty(item.getTierId(), item.getQuantity());
        }

        walletRepository.creditWallet(buyerId, order.getTotalAmount());

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);

        auditService.logOrderCancelled(orderId);

        BigDecimal remainingBalance = walletRepository.getBalance(buyerId);
        String message = "Order cancelled. ₹" + order.getTotalAmount() +
                " credited back to your wallet within 1 day.";
        return new CancelOrderResponse(orderId, "CANCELLED", message, remainingBalance);
    }

    @Transactional(readOnly = true)
    public OrderHistoryResponse getMyOrders(Long buyerId, Integer page, Integer size) {
        if (page < 0) {
            page = 0;
        }
        if (size < 1 || size > 100) {
            size = 10;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orderPage = orderRepository.findByBuyerIdAndStatus(buyerId, OrderStatus.CONFIRMED, pageable);

        List<OrderSummary> summaries = orderPage.getContent().stream()
                .map(this::mapToOrderSummary)
                .collect(Collectors.toList());

        return new OrderHistoryResponse(
                summaries,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderById(Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.getBuyerId().equals(buyerId)) {
            throw new OrderAccessDeniedException(orderId);
        }
        return mapToOrderDetail(order);
    }

    private OrderSummary mapToOrderSummary(Order order) {
        List<OrderItemSummary> items = order.getItems().stream()
                .map(item -> new OrderItemSummary(
                        item.getId(),
                        item.getTierName(),
                        item.getEventTitle(),
                        item.getEventDate(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getVenueName()
                ))
                .collect(Collectors.toList());

        return new OrderSummary(
                order.getId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                items
        );
    }

    private OrderDetailResponse mapToOrderDetail(Order order) {
        List<OrderItemDetail> items = order.getItems().stream()
                .map(item -> new OrderItemDetail(
                        item.getId(),
                        item.getTierId(),
                        item.getTierName(),
                        item.getEventTitle(),
                        item.getEventDate(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return new OrderDetailResponse(
                order.getId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getPaymentLinkId(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }
}
