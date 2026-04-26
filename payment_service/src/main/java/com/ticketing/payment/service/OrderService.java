package com.ticketing.payment.service;

import com.ticketing.payment.dto.CreateOrderRequest;
import com.ticketing.payment.dto.CreateOrderResponse;
import com.ticketing.payment.entity.*;
import com.ticketing.payment.exception.InsufficientInventoryException;
import com.ticketing.payment.exception.InvalidTierException;
import com.ticketing.payment.repository.OrderItemRepository;
import com.ticketing.payment.repository.OrderRepository;
import com.ticketing.payment.repository.TicketTierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TicketTierRepository ticketTierRepository;
    private final AuditService auditService;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        TicketTierRepository ticketTierRepository,
                        AuditService auditService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.ticketTierRepository = ticketTierRepository;
        this.auditService = auditService;
    }

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request, UUID buyerId) {
        TicketTier tier = ticketTierRepository.findById(request.getTierId())
                .orElseThrow(() -> new InvalidTierException("Tier not found with ID: " + request.getTierId()));

        if (tier.getStatus() != TierStatus.ACTIVE) {
            throw new InvalidTierException("Tier is not active");
        }

        if (tier.getRemainingQty() < request.getQuantity()) {
            throw new InsufficientInventoryException(
                    "Insufficient inventory. Requested: " + request.getQuantity() +
                    ", Available: " + tier.getRemainingQty());
        }

        if (request.getQuantity() > tier.getMaxPerOrder()) {
            throw new InvalidTierException(
                    "Quantity exceeds maximum per order limit: " + tier.getMaxPerOrder());
        }

        BigDecimal totalAmount = tier.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(totalAmount);
        order = orderRepository.save(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setTierId(tier.getId());
        orderItem.setQuantity(request.getQuantity());
        orderItem.setTierName(tier.getName());
        orderItem.setEventTitle(tier.getEventTitle());
        orderItem.setEventDate(tier.getEventDate());
        orderItem.setUnitPrice(tier.getPrice());
        orderItemRepository.save(orderItem);

        auditService.logOrderCreated(
                order.getId().toString(),
                tier.getId().toString(),
                request.getQuantity(),
                buyerId.toString());

        return new CreateOrderResponse(order.getId(), null);
    }
}
