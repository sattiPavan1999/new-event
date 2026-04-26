package com.ticketing.payment.service;

import com.ticketing.payment.dto.CreateOrderRequest;
import com.ticketing.payment.dto.CreateOrderResponse;
import com.ticketing.payment.entity.*;
import com.ticketing.payment.exception.InsufficientInventoryException;
import com.ticketing.payment.exception.InvalidTierException;
import com.ticketing.payment.repository.OrderItemRepository;
import com.ticketing.payment.repository.OrderRepository;
import com.ticketing.payment.repository.TicketTierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentOrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private TicketTierRepository ticketTierRepository;
    @Mock private RazorpayService razorpayService;
    @Mock private AuditService auditService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderItemRepository,
                ticketTierRepository, razorpayService, auditService);
        ReflectionTestUtils.setField(orderService, "successUrl", "http://localhost:3000/success");
        ReflectionTestUtils.setField(orderService, "cancelUrl", "http://localhost:3000/cancel");
    }

    @Test
    void createOrder_validTier_returnsResponseWithCheckoutUrl() {
        UUID tierId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        TicketTier tier = buildTier(tierId, 50, 10, new BigDecimal("500.00"));

        Order savedOrder = new Order();
        savedOrder.setId(UUID.randomUUID());
        savedOrder.setBuyerId(buyerId);
        savedOrder.setStatus(OrderStatus.PENDING);
        savedOrder.setTotalAmount(new BigDecimal("1000.00"));

        when(ticketTierRepository.findById(tierId)).thenReturn(Optional.of(tier));
        when(orderRepository.save(any())).thenReturn(savedOrder);
        when(orderItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(razorpayService.createPaymentLink(any(), any(), any(), anyInt(), any(), any()))
                .thenReturn("https://rzp.io/l/abc");
        when(razorpayService.extractPaymentLinkId("https://rzp.io/l/abc")).thenReturn("abc");

        CreateOrderRequest request = new CreateOrderRequest(tierId, 2);
        CreateOrderResponse response = orderService.createOrder(request, buyerId);

        assertNotNull(response);
        assertEquals("https://rzp.io/l/abc", response.getCheckoutUrl());
        verify(auditService).logOrderCreated(any(), any(), anyInt(), any());
        verify(auditService).logPaymentLinkCreated(any(), any());
    }

    @Test
    void createOrder_tierNotFound_throwsInvalidTierException() {
        UUID tierId = UUID.randomUUID();
        when(ticketTierRepository.findById(tierId)).thenReturn(Optional.empty());

        assertThrows(InvalidTierException.class,
                () -> orderService.createOrder(new CreateOrderRequest(tierId, 1), UUID.randomUUID()));
    }

    @Test
    void createOrder_tierInactive_throwsInvalidTierException() {
        UUID tierId = UUID.randomUUID();
        TicketTier tier = buildTier(tierId, 50, 5, new BigDecimal("500.00"));
        tier.setStatus(TierStatus.INACTIVE);
        when(ticketTierRepository.findById(tierId)).thenReturn(Optional.of(tier));

        assertThrows(InvalidTierException.class,
                () -> orderService.createOrder(new CreateOrderRequest(tierId, 1), UUID.randomUUID()));
    }

    @Test
    void createOrder_insufficientInventory_throwsInsufficientInventoryException() {
        UUID tierId = UUID.randomUUID();
        TicketTier tier = buildTier(tierId, 10, 5, new BigDecimal("500.00"));
        tier.setRemainingQty(1);
        when(ticketTierRepository.findById(tierId)).thenReturn(Optional.of(tier));

        assertThrows(InsufficientInventoryException.class,
                () -> orderService.createOrder(new CreateOrderRequest(tierId, 3), UUID.randomUUID()));
    }

    @Test
    void createOrder_quantityExceedsMaxPerOrder_throwsInvalidTierException() {
        UUID tierId = UUID.randomUUID();
        TicketTier tier = buildTier(tierId, 100, 3, new BigDecimal("500.00"));
        when(ticketTierRepository.findById(tierId)).thenReturn(Optional.of(tier));

        assertThrows(InvalidTierException.class,
                () -> orderService.createOrder(new CreateOrderRequest(tierId, 5), UUID.randomUUID()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TicketTier buildTier(UUID id, int totalQty, int maxPerOrder, BigDecimal price) {
        TicketTier tier = new TicketTier();
        tier.setId(id);
        tier.setEventId(UUID.randomUUID());
        tier.setName("General");
        tier.setPrice(price);
        tier.setTotalQty(totalQty);
        tier.setRemainingQty(totalQty);
        tier.setMaxPerOrder(maxPerOrder);
        tier.setStatus(TierStatus.ACTIVE);
        tier.setEventTitle("Test Event");
        tier.setEventDate(LocalDateTime.now().plusDays(30));
        return tier;
    }
}
