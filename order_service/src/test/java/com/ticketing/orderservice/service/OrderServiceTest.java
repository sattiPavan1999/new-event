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
        Long buyerId = 1L;
        Long eventId = 10L;
        Long tierId = 100L;

        EventServiceResponse event = buildEvent(eventId, tierId);
        when(eventServiceClient.getEvent(eventId)).thenReturn(Optional.of(event));
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(50L);
            return o;
        });
        when(walletRepository.debitWallet(eq(buyerId), any())).thenReturn(1);
        when(walletRepository.getBalance(buyerId)).thenReturn(new BigDecimal("7000.00"));
        when(ticketTierRepository.decrementRemainingQty(any(), anyInt())).thenReturn(1);

        CreateOrderRequest request = buildOrderRequest(eventId, tierId, 2);

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
        Long eventId = 10L;
        when(eventServiceClient.getEvent(eventId)).thenReturn(Optional.empty());

        CreateOrderRequest request = buildOrderRequest(eventId, 100L, 1);

        assertThrows(EventNotFoundException.class,
                () -> orderService.createOrder(request, 1L));
    }

    @Test
    void createOrder_eventNotPublished_throwsInvalidEventStatusException() {
        Long eventId = 10L;
        Long tierId = 100L;
        EventServiceResponse event = buildEvent(eventId, tierId);
        event.setStatus("DRAFT");
        when(eventServiceClient.getEvent(eventId)).thenReturn(Optional.of(event));

        CreateOrderRequest request = buildOrderRequest(eventId, tierId, 1);

        assertThrows(InvalidEventStatusException.class,
                () -> orderService.createOrder(request, 1L));
    }

    @Test
    void createOrder_tierNotInEvent_throwsTierNotFoundException() {
        Long eventId = 10L;
        Long tierId = 100L;
        Long wrongTierId = 999L;
        EventServiceResponse event = buildEvent(eventId, tierId);
        when(eventServiceClient.getEvent(eventId)).thenReturn(Optional.of(event));

        CreateOrderRequest request = buildOrderRequest(eventId, wrongTierId, 1);

        assertThrows(TierNotFoundException.class,
                () -> orderService.createOrder(request, 1L));
    }

    @Test
    void createOrder_quantityExceedsMax_throwsQuantityExceedsMaxException() {
        Long eventId = 10L;
        Long tierId = 100L;
        EventServiceResponse event = buildEvent(eventId, tierId);
        when(eventServiceClient.getEvent(eventId)).thenReturn(Optional.of(event));

        CreateOrderRequest request = buildOrderRequest(eventId, tierId, 10);

        assertThrows(QuantityExceedsMaxPerOrderException.class,
                () -> orderService.createOrder(request, 1L));
    }

    @Test
    void createOrder_insufficientInventory_throwsInsufficientInventoryException() {
        Long eventId = 10L;
        Long tierId = 100L;
        EventServiceResponse event = buildEvent(eventId, tierId);
        event.getTiers().get(0).setRemainingQty(2);
        when(eventServiceClient.getEvent(eventId)).thenReturn(Optional.of(event));

        CreateOrderRequest request = buildOrderRequest(eventId, tierId, 3);

        assertThrows(InsufficientInventoryException.class,
                () -> orderService.createOrder(request, 1L));
    }

    // ── getMyOrders ───────────────────────────────────────────────────────────

    @Test
    void getMyOrders_returnsConfirmedOrdersPage() {
        Long buyerId = 1L;
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
        Long buyerId = 1L;
        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList());
        when(orderRepository.findByBuyerIdAndStatus(any(), any(), any())).thenReturn(emptyPage);

        OrderHistoryResponse response = orderService.getMyOrders(buyerId, 0, 10);

        assertEquals(0, response.getTotalElements());
    }

    @Test
    void getMyOrders_invalidPageParams_normalizes() {
        Long buyerId = 1L;
        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList());
        when(orderRepository.findByBuyerIdAndStatus(any(), any(), any())).thenReturn(emptyPage);

        assertDoesNotThrow(() -> orderService.getMyOrders(buyerId, -5, 200));
    }

    // ── getOrderById ──────────────────────────────────────────────────────────

    @Test
    void getOrderById_ownOrder_returnsDetail() {
        Long buyerId = 1L;
        Order order = buildOrder(buyerId, OrderStatus.CONFIRMED);
        when(orderRepository.findByIdAndBuyerId(order.getId(), buyerId))
                .thenReturn(Optional.of(order));

        OrderDetailResponse response = orderService.getOrderById(order.getId(), buyerId);

        assertNotNull(response);
        assertEquals("CONFIRMED", response.getStatus());
    }

    @Test
    void getOrderById_orderBelongsToOtherBuyer_throwsOrderAccessDeniedException() {
        Long orderId = 50L;
        Long buyerId = 1L;
        when(orderRepository.findByIdAndBuyerId(orderId, buyerId)).thenReturn(Optional.empty());
        when(orderRepository.existsById(orderId)).thenReturn(true);

        assertThrows(OrderAccessDeniedException.class,
                () -> orderService.getOrderById(orderId, buyerId));
    }

    @Test
    void getOrderById_orderDoesNotExist_throwsOrderNotFoundException() {
        Long orderId = 50L;
        Long buyerId = 1L;
        when(orderRepository.findByIdAndBuyerId(orderId, buyerId)).thenReturn(Optional.empty());
        when(orderRepository.existsById(orderId)).thenReturn(false);

        assertThrows(OrderNotFoundException.class,
                () -> orderService.getOrderById(orderId, buyerId));
    }

    // ── cancelOrder ──────────────────────────────────────────────────────────

    @Test
    void cancelOrder_confirmedOrder_returnsCancelled() {
        Long buyerId = 1L;
        Long tierId = 100L;
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
        Long buyerId = 1L;
        Order order = buildOrderWithItem(buyerId, OrderStatus.PENDING, 100L,
                Instant.now().plus(2, ChronoUnit.DAYS));

        when(orderRepository.findByIdAndBuyerId(order.getId(), buyerId))
                .thenReturn(Optional.of(order));

        assertThrows(OrderCancellationNotAllowedException.class,
                () -> orderService.cancelOrder(order.getId(), buyerId));
    }

    @Test
    void cancelOrder_eventTooSoon_throwsCancellationNotAllowed() {
        Long buyerId = 1L;
        Order order = buildOrderWithItem(buyerId, OrderStatus.CONFIRMED, 100L,
                Instant.now().plus(12, ChronoUnit.HOURS));

        when(orderRepository.findByIdAndBuyerId(order.getId(), buyerId))
                .thenReturn(Optional.of(order));

        assertThrows(OrderCancellationNotAllowedException.class,
                () -> orderService.cancelOrder(order.getId(), buyerId));
    }

    @Test
    void cancelOrder_notOwner_throwsOrderNotFoundException() {
        Long orderId = 50L;
        Long buyerId = 1L;
        when(orderRepository.findByIdAndBuyerId(orderId, buyerId)).thenReturn(Optional.empty());
        when(orderRepository.existsById(orderId)).thenReturn(false);

        assertThrows(OrderNotFoundException.class,
                () -> orderService.cancelOrder(orderId, buyerId));
    }

    @Test
    void createOrder_insufficientBalance_throwsInsufficientWalletBalanceException() {
        Long buyerId = 1L;
        Long eventId = 10L;
        Long tierId = 100L;

        EventServiceResponse event = buildEvent(eventId, tierId);
        when(eventServiceClient.getEvent(eventId)).thenReturn(Optional.of(event));
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(50L);
            return o;
        });
        when(walletRepository.debitWallet(eq(buyerId), any())).thenReturn(0);

        CreateOrderRequest request = buildOrderRequest(eventId, tierId, 2);

        assertThrows(InsufficientWalletBalanceException.class,
                () -> orderService.createOrder(request, buyerId));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CreateOrderRequest buildOrderRequest(Long eventId, Long tierId, int qty) {
        OrderItemRequest item = new OrderItemRequest();
        item.setTierId(tierId);
        item.setQuantity(qty);
        CreateOrderRequest request = new CreateOrderRequest();
        request.setEventId(eventId);
        request.setItems(List.of(item));
        return request;
    }

    private EventServiceResponse buildEvent(Long eventId, Long tierId) {
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

    private Order buildOrder(Long buyerId, OrderStatus status) {
        Order order = new Order();
        order.setId(1L);
        order.setBuyerId(buyerId);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("3000.00"));
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        return order;
    }

    private Order buildOrderWithItem(Long buyerId, OrderStatus status, Long tierId, Instant eventDate) {
        Order order = buildOrder(buyerId, status);
        OrderItem item = new OrderItem();
        item.setId(10L);
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
