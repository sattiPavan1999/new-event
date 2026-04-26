package com.ticketing.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.orderservice.entity.Order;
import com.ticketing.orderservice.entity.OrderItem;
import com.ticketing.orderservice.entity.OrderStatus;
import com.ticketing.orderservice.repository.OrderRepository;
import com.ticketing.orderservice.repository.TicketTierRepository;
import com.ticketing.orderservice.util.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private TicketTierRepository ticketTierRepository;
    @Mock private AuditService auditService;

    private PaymentWebhookService webhookService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        webhookService = new PaymentWebhookService(
                orderRepository, ticketTierRepository, auditService, objectMapper);
    }

    @Test
    void processWebhook_paymentLinkPaid_confirmsOrder() {
        UUID orderId = UUID.randomUUID();
        UUID tierId = UUID.randomUUID();

        Order order = buildOrder(orderId, OrderStatus.PENDING);
        OrderItem item = buildOrderItem(tierId, 2);
        order.setItems(List.of(item));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(ticketTierRepository.decrementRemainingQty(tierId, 2)).thenReturn(1);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String payload = buildPaymentLinkPaidPayload(orderId, "pay_123");
        webhookService.processWebhook(payload);

        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.CONFIRMED));
        verify(auditService).logOrderConfirmed(orderId);
    }

    @Test
    void processWebhook_paymentLinkPaid_insufficientInventory_failsOrder() {
        UUID orderId = UUID.randomUUID();
        UUID tierId = UUID.randomUUID();

        Order order = buildOrder(orderId, OrderStatus.PENDING);
        OrderItem item = buildOrderItem(tierId, 5);
        order.setItems(List.of(item));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(ticketTierRepository.decrementRemainingQty(tierId, 5)).thenReturn(0);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String payload = buildPaymentLinkPaidPayload(orderId, "pay_456");
        webhookService.processWebhook(payload);

        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.FAILED));
    }

    @Test
    void processWebhook_paymentLinkPaid_alreadyConfirmed_skips() {
        UUID orderId = UUID.randomUUID();
        Order order = buildOrder(orderId, OrderStatus.CONFIRMED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        String payload = buildPaymentLinkPaidPayload(orderId, "pay_789");
        webhookService.processWebhook(payload);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void processWebhook_paymentFailed_setsFailedStatus() {
        UUID orderId = UUID.randomUUID();
        Order order = buildOrder(orderId, OrderStatus.PENDING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String payload = buildPaymentFailedPayload(orderId, "pay_fail");
        webhookService.processWebhook(payload);

        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.FAILED));
    }

    @Test
    void processWebhook_invalidJson_doesNotThrow() {
        // Malformed JSON - service swallows the error
        webhookService.processWebhook("not-valid-json{");
        verifyNoInteractions(orderRepository);
    }

    @Test
    void processWebhook_unknownEventType_doesNothing() {
        String payload = "{\"event\":\"unknown.event\",\"payload\":{}}";
        webhookService.processWebhook(payload);
        verifyNoInteractions(orderRepository);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Order buildOrder(UUID orderId, OrderStatus status) {
        Order order = new Order();
        order.setId(orderId);
        order.setBuyerId(UUID.randomUUID());
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("3000.00"));
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        return order;
    }

    private OrderItem buildOrderItem(UUID tierId, int quantity) {
        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setTierId(tierId);
        item.setQuantity(quantity);
        item.setTierName("General");
        item.setUnitPrice(new BigDecimal("1000.00"));
        item.setCreatedAt(Instant.now());
        return item;
    }

    private String buildPaymentLinkPaidPayload(UUID orderId, String paymentId) {
        return String.format("""
                {
                  "event": "payment_link.paid",
                  "payload": {
                    "payment_link": {
                      "entity": {
                        "notes": { "orderId": "%s" }
                      }
                    },
                    "payment": {
                      "entity": { "id": "%s" }
                    }
                  }
                }""", orderId, paymentId);
    }

    private String buildPaymentFailedPayload(UUID orderId, String paymentId) {
        return String.format("""
                {
                  "event": "payment.failed",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "%s",
                        "notes": { "orderId": "%s" }
                      }
                    }
                  }
                }""", paymentId, orderId);
    }
}
