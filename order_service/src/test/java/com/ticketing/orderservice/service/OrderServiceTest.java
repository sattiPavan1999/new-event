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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private EventServiceClient eventServiceClient;
    @Mock private TicketTierRepository ticketTierRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private AuditService auditService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, eventServiceClient,
                ticketTierRepository, walletRepository, auditService);
        ReflectionTestUtils.setField(orderService, "mockPaymentCheckout", true);
    }

    // ── createOrder ───────────────────────────────────────────────────────────

    @Test
    void createOrder_mockCheckout_returnsConfirmedOrder() {
        UUID buyerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID tierId = UUID.randomUUID();

        EventServiceResponse event = buildEvent(eventId, tierId);
        when(eventServiceClient.getEvent(eventId)).thenReturn(Optional.of(event));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletRepository.debitWallet(eq(buyerId), any())).thenReturn(1);
        when(walletRepository.getBalance(buyerId)).thenReturn(new BigDecimal("7000.00"));
        when(ticketTierRepository.decrementRemainingQty(any(), anyInt())).thenReturn(1);

        CreateOrderRequest request = new CreateOrderRequest(eventId,
                List.of(new OrderItemRequest(tierId, 2)));

        CreateOrderResponse response = orderService.createOrder(request, buyerId);

        assertNotNull(response);
        assertEquals("CONFIRMED", response.getStatus());
        assertEquals(new BigDecimal("3000.00"), response.getTotalAmount());
        assertEquals(new BigDecimal("7000.00"), response.getRemainingBalance());
        verify(walletRepository).debitWallet(eq(buyerId), eq(new BigDecimal("3000.00")));
        verify(auditService).logOrderCreated(any(), eq(buyerId), eq(1), anyString());
        verify(auditService).logOrderConfirmed(any());
    }

    @Test
    void createOrder_eventNotFound_throwsEventNotFoundException() {
        UUID eventId = UUID.randomUUID();
        when(eventServiceClient.getEvent(eventId)).thenReturn(Optional.empty());

        CreateOrderRequest request = new CreateOrderRequest(eventId,
                List.of(new OrderItemRequest(UUID.randomUUID(), 1)));

        assertThrows(EventNotFoundException.class,
                () -> orderService.createOrder(request, UUID.randomUUID()));
    }

    @Test
    void createOrder_eventNotPublished_throwsInvalidEventStatusException() {
        UUID eventId = UUID.randomUUID();
        UUID tierId = UUID.randomUUID();
        EventServiceResponse event = buildEvent(eventId, tierId);
        event.setStatus("DRAFT");
        when(eventServiceClient.getEvent(eventId)).thenReturn(Optional.of(event));

        CreateOrderRequest request = new CreateOrderRequest(eventId,
                List.of(new OrderItemRequest(tierId, 1)));

        assertThrows(InvalidEventStatusException.class,
                () -> orderService.createOrder(request, UUID.randomUUID()));
    }

    @Test
    void createOrder_tierNotInEvent_throwsTierNotFoundException() {
        UUID eventId = UUID.randomUUID();
        UUID tierId = UUID.randomUUID();
        UUID wrongTierId = UUID.randomUUID();
        EventServiceResponse event = buildEvent(eventId, tierId);
        when(eventServiceClient.getEvent(eventId)).thenReturn(Optional.of(event));

        CreateOrderRequest request = new CreateOrderRequest(eventId,
                List.of(new OrderItemRequest(wrongTierId, 1)));

        assertThrows(TierNotFoundException.class,
                () -> orderService.createOrder(request, UUID.randomUUID()));
    }

    @Test
    void createOrder_quantityExceedsMax_throwsQuantityExceedsMaxException() {
        UUID eventId = UUID.randomUUID();
        UUID tierId = UUID.randomUUID();
        EventServiceResponse event = buildEvent(eventId, tierId);
        when(eventServiceClient.getEvent(eventId)).thenReturn(Optional.of(event));

        CreateOrderRequest request = new CreateOrderRequest(eventId,
                List.of(new OrderItemRequest(tierId, 10)));

        assertThrows(QuantityExceedsMaxPerOrderException.class,
                () -> orderService.createOrder(request, UUID.randomUUID()));
    }

    @Test
    void createOrder_insufficientInventory_throwsInsufficientInventoryException() {
        UUID eventId = UUID.randomUUID();
        UUID tierId = UUID.randomUUID();
        EventServiceResponse event = buildEvent(eventId, tierId);
        event.getTiers().get(0).setRemainingQty(2);
        when(eventServiceClient.getEvent(eventId)).thenReturn(Optional.of(event));

        CreateOrderRequest request = new CreateOrderRequest(eventId,
                List.of(new OrderItemRequest(tierId, 3)));

        assertThrows(InsufficientInventoryException.class,
                () -> orderService.createOrder(request, UUID.randomUUID()));
    }

    // ── getMyOrders ───────────────────────────────────────────────────────────

    @Test
    void getMyOrders_returnsConfirmedOrdersPage() {
        UUID buyerId = UUID.randomUUID();
        Order order = buildOrder(buyerId, OrderStatus.CONFIRMED);
        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderRepository.findByBuyerIdAndStatus(eq(buyerId), eq(OrderStatus.CONFIRMED), any()))
                .thenReturn(page);

        OrderHistoryResponse response = orderService.getMyOrders(buyerId, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(0, response.getPage());
    }

    @Test
    void getMyOrders_emptyResult_returnsEmptyPage() {
        UUID buyerId = UUID.randomUUID();
        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList());
        when(orderRepository.findByBuyerIdAndStatus(any(), any(), any())).thenReturn(emptyPage);

        OrderHistoryResponse response = orderService.getMyOrders(buyerId, 0, 10);

        assertEquals(0, response.getTotalElements());
    }

    @Test
    void getMyOrders_invalidPageParams_normalizes() {
        UUID buyerId = UUID.randomUUID();
        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList());
        when(orderRepository.findByBuyerIdAndStatus(any(), any(), any())).thenReturn(emptyPage);

        assertDoesNotThrow(() -> orderService.getMyOrders(buyerId, -5, 200));
    }

    // ── getOrderById ──────────────────────────────────────────────────────────

    @Test
    void getOrderById_ownOrder_returnsDetail() {
        UUID buyerId = UUID.randomUUID();
        Order order = buildOrder(buyerId, OrderStatus.CONFIRMED);
        when(orderRepository.findByIdAndBuyerId(order.getId(), buyerId))
                .thenReturn(Optional.of(order));

        OrderDetailResponse response = orderService.getOrderById(order.getId(), buyerId);

        assertNotNull(response);
        assertEquals("CONFIRMED", response.getStatus());
    }

    @Test
    void getOrderById_orderBelongsToOtherBuyer_throwsOrderAccessDeniedException() {
        UUID orderId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        when(orderRepository.findByIdAndBuyerId(orderId, buyerId)).thenReturn(Optional.empty());
        when(orderRepository.existsById(orderId)).thenReturn(true);

        assertThrows(OrderAccessDeniedException.class,
                () -> orderService.getOrderById(orderId, buyerId));
    }

    @Test
    void getOrderById_orderDoesNotExist_throwsOrderNotFoundException() {
        UUID orderId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        when(orderRepository.findByIdAndBuyerId(orderId, buyerId)).thenReturn(Optional.empty());
        when(orderRepository.existsById(orderId)).thenReturn(false);

        assertThrows(OrderNotFoundException.class,
                () -> orderService.getOrderById(orderId, buyerId));
    }

    // ── cancelOrder ──────────────────────────────────────────────────────────

    @Test
    void cancelOrder_confirmedOrder_returnsCancelled() {
        UUID buyerId = UUID.randomUUID();
        UUID tierId = UUID.randomUUID();
        Order order = buildOrderWithItem(buyerId, OrderStatus.CONFIRMED, tierId,
                Instant.now().plus(2, ChronoUnit.DAYS));

        when(orderRepository.findByIdAndBuyerId(order.getId(), buyerId))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ticketTierRepository.incrementRemainingQty(any(), anyInt())).thenReturn(1);
        when(walletRepository.creditWallet(eq(buyerId), any())).thenReturn(1);
        when(walletRepository.getBalance(buyerId)).thenReturn(new BigDecimal("10000.00"));

        CancelOrderResponse response = orderService.cancelOrder(order.getId(), buyerId);

        assertEquals("CANCELLED", response.getStatus());
        assertEquals(new BigDecimal("10000.00"), response.getRemainingBalance());
        verify(ticketTierRepository).incrementRemainingQty(eq(tierId), eq(1));
        verify(walletRepository).creditWallet(eq(buyerId), any());
        verify(auditService).logOrderCancelled(order.getId());
    }

    @Test
    void cancelOrder_pendingOrder_throwsCancellationNotAllowed() {
        UUID buyerId = UUID.randomUUID();
        Order order = buildOrderWithItem(buyerId, OrderStatus.PENDING, UUID.randomUUID(),
                Instant.now().plus(2, ChronoUnit.DAYS));

        when(orderRepository.findByIdAndBuyerId(order.getId(), buyerId))
                .thenReturn(Optional.of(order));

        assertThrows(OrderCancellationNotAllowedException.class,
                () -> orderService.cancelOrder(order.getId(), buyerId));
    }

    @Test
    void cancelOrder_eventTooSoon_throwsCancellationNotAllowed() {
        UUID buyerId = UUID.randomUUID();
        Order order = buildOrderWithItem(buyerId, OrderStatus.CONFIRMED, UUID.randomUUID(),
                Instant.now().plus(12, ChronoUnit.HOURS));

        when(orderRepository.findByIdAndBuyerId(order.getId(), buyerId))
                .thenReturn(Optional.of(order));

        assertThrows(OrderCancellationNotAllowedException.class,
                () -> orderService.cancelOrder(order.getId(), buyerId));
    }

    @Test
    void cancelOrder_notOwner_throwsOrderNotFoundException() {
        UUID orderId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        when(orderRepository.findByIdAndBuyerId(orderId, buyerId)).thenReturn(Optional.empty());
        when(orderRepository.existsById(orderId)).thenReturn(false);

        assertThrows(OrderNotFoundException.class,
                () -> orderService.cancelOrder(orderId, buyerId));
    }

    @Test
    void createOrder_insufficientBalance_throwsInsufficientWalletBalanceException() {
        UUID buyerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID tierId = UUID.randomUUID();

        EventServiceResponse event = buildEvent(eventId, tierId);
        when(eventServiceClient.getEvent(eventId)).thenReturn(Optional.of(event));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletRepository.debitWallet(eq(buyerId), any())).thenReturn(0);

        CreateOrderRequest request = new CreateOrderRequest(eventId,
                List.of(new OrderItemRequest(tierId, 2)));

        assertThrows(InsufficientWalletBalanceException.class,
                () -> orderService.createOrder(request, buyerId));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private EventServiceResponse buildEvent(UUID eventId, UUID tierId) {
        EventServiceResponse.TierResponse tier = new EventServiceResponse.TierResponse();
        tier.setId(tierId);
        tier.setName("General");
        tier.setPrice(new BigDecimal("1500.00"));
        tier.setTotalQty(100);
        tier.setRemainingQty(50);
        tier.setMaxPerOrder(5);
        tier.setStatus("ACTIVE");

        EventServiceResponse event = new EventServiceResponse();
        event.setId(eventId);
        event.setTitle("Test Concert");
        event.setStatus("PUBLISHED");
        event.setEventDate("2026-12-01T18:00:00");
        event.setTiers(List.of(tier));
        return event;
    }

    private Order buildOrder(UUID buyerId, OrderStatus status) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setBuyerId(buyerId);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("3000.00"));
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        return order;
    }

    private Order buildOrderWithItem(UUID buyerId, OrderStatus status, UUID tierId, Instant eventDate) {
        Order order = buildOrder(buyerId, status);
        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setOrder(order);
        item.setTierId(tierId);
        item.setTierName("General");
        item.setEventTitle("Test Event");
        item.setEventDate(eventDate);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("3000.00"));
        item.setCreatedAt(Instant.now());
        order.getItems().add(item);
        return order;
    }
}
