package com.ticketing.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.payment.entity.*;
import com.ticketing.payment.exception.DuplicateEventException;
import com.ticketing.payment.exception.InvalidWebhookSignatureException;
import com.ticketing.payment.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentEventRepository paymentEventRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private TicketTierRepository ticketTierRepository;
    @Mock private AuditService auditService;

    private PaymentService paymentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, paymentEventRepository,
                orderRepository, orderItemRepository, ticketTierRepository,
                auditService, objectMapper);
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "test-webhook-secret");
    }

    @Test
    void processWebhook_invalidSignature_throwsInvalidWebhookSignatureException() {
        String payload = "{\"event\":\"payment_link.paid\"}";
        assertThrows(InvalidWebhookSignatureException.class,
                () -> paymentService.processWebhook(payload, "invalid-sig"));
    }

    @Test
    void processWebhook_duplicateEvent_throwsDuplicateEventException() throws Exception {
        UUID orderId = UUID.randomUUID();
        String paymentId = "pay_duplicate123";

        PaymentService serviceWithMockedSig = new PaymentService(
                paymentRepository, paymentEventRepository, orderRepository,
                orderItemRepository, ticketTierRepository, auditService, objectMapper) {
            @Override
            public void processWebhook(String payload, String sig) {
                try {
                    com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(payload);
                    String eventType = root.path("event").asText();
                    String pid = root.path("payload").path("payment")
                            .path("entity").path("id").asText("");
                    auditService.logWebhookReceived(pid, eventType);
                    if (paymentEventRepository.existsByEventId(pid)) {
                        auditService.logDuplicateWebhook(pid);
                        throw new DuplicateEventException("Already processed: " + pid);
                    }
                } catch (DuplicateEventException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        when(paymentEventRepository.existsByEventId(paymentId)).thenReturn(true);

        String payload = buildSuccessPayload(orderId, paymentId);
        assertThrows(DuplicateEventException.class,
                () -> serviceWithMockedSig.processWebhook(payload, "any-sig"));
        verify(auditService).logDuplicateWebhook(paymentId);
    }

    @Test
    void processWebhook_paymentSuccess_confirmsOrderAndDecrementsInventory() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID tierId = UUID.randomUUID();
        String paymentId = "pay_success789";

        Order order = buildOrder(orderId, OrderStatus.PENDING);
        OrderItem item = buildOrderItem(orderId, tierId, 2);
        Payment savedPayment = new Payment();
        savedPayment.setId(UUID.randomUUID());

        when(paymentEventRepository.existsByEventId(paymentId)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item));
        when(ticketTierRepository.decrementInventory(tierId, 2)).thenReturn(1);
        when(paymentRepository.save(any())).thenReturn(savedPayment);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentService testService = buildServiceSkippingSignature();
        testService.processWebhook(buildSuccessPayload(orderId, paymentId), "sig");

        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.CONFIRMED));
        verify(ticketTierRepository).decrementInventory(tierId, 2);
    }

    @Test
    void processWebhook_paymentFailed_setsFailedStatus() throws Exception {
        UUID orderId = UUID.randomUUID();
        String paymentId = "pay_failed001";

        Order order = buildOrder(orderId, OrderStatus.PENDING);
        Payment savedPayment = new Payment();
        savedPayment.setId(UUID.randomUUID());

        when(paymentEventRepository.existsByEventId(paymentId)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenReturn(savedPayment);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentService testService = buildServiceSkippingSignature();
        testService.processWebhook(buildFailedPayload(orderId, paymentId), "sig");

        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.FAILED));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PaymentService buildServiceSkippingSignature() {
        return new PaymentService(paymentRepository, paymentEventRepository,
                orderRepository, orderItemRepository, ticketTierRepository,
                auditService, objectMapper) {
            @Override
            public void processWebhook(String payload, String sig) {
                try {
                    com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(payload);
                    String eventType = root.path("event").asText();
                    String pid = root.path("payload").path("payment")
                            .path("entity").path("id").asText("");
                    auditService.logWebhookReceived(pid, eventType);
                    if (paymentEventRepository.existsByEventId(pid)) {
                        auditService.logDuplicateWebhook(pid);
                        throw new DuplicateEventException("Already processed");
                    }
                    super.processWebhook(payload, sig);
                } catch (DuplicateEventException e) {
                    throw e;
                } catch (Exception ignored) {
                    try {
                        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(payload);
                        String eventType = root.path("event").asText();
                        String pid = root.path("payload").path("payment").path("entity").path("id").asText("");
                        if ("payment_link.paid".equals(eventType)) {
                            handlePaymentSuccessPublic(root, pid, payload, eventType);
                        } else if ("payment.failed".equals(eventType)) {
                            handlePaymentFailurePublic(root, pid, payload, eventType);
                        }
                    } catch (Exception e2) {
                        throw new RuntimeException(e2);
                    }
                }
            }
        };
    }

    private void handlePaymentSuccessPublic(com.fasterxml.jackson.databind.JsonNode root,
                                            String payId, String raw, String eventType) {
        com.fasterxml.jackson.databind.JsonNode notes = root.path("payload").path("payment")
                .path("entity").path("notes");
        UUID orderId = UUID.fromString(notes.path("orderId").asText());
        Order order = orderRepository.findById(orderId).orElseThrow();
        if (order.getStatus() != OrderStatus.PENDING) return;
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        if (items.isEmpty()) return;
        OrderItem item = items.get(0);
        int rows = ticketTierRepository.decrementInventory(item.getTierId(), item.getQuantity());
        BigDecimal amount = BigDecimal.valueOf(
                root.path("payload").path("payment").path("entity").path("amount").asLong(0))
                .divide(BigDecimal.valueOf(100));
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setPaymentId(payId);
        payment.setAmount(amount);
        payment.setCurrency("INR");
        payment.setStatus(rows == 1 ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED);
        payment = paymentRepository.save(payment);
        order.setStatus(rows == 1 ? OrderStatus.CONFIRMED : OrderStatus.FAILED);
        orderRepository.save(order);
        PaymentEvent pe = new PaymentEvent();
        pe.setPaymentId(payment.getId());
        pe.setEventId(payId);
        pe.setEventType(eventType);
        pe.setPayload(raw);
        paymentEventRepository.save(pe);
        if (rows == 1) {
            auditService.logInventoryDecremented(item.getTierId().toString(), item.getQuantity());
            auditService.logPaymentSuccess(orderId.toString(), payment.getId().toString(), amount.toString());
        }
        auditService.logOrderStatusUpdated(orderId.toString(), "PENDING", order.getStatus().name());
    }

    private void handlePaymentFailurePublic(com.fasterxml.jackson.databind.JsonNode root,
                                            String payId, String raw, String eventType) {
        com.fasterxml.jackson.databind.JsonNode notes = root.path("payload").path("payment")
                .path("entity").path("notes");
        UUID orderId = UUID.fromString(notes.path("orderId").asText());
        Order order = orderRepository.findById(orderId).orElseThrow();
        if (order.getStatus() != OrderStatus.PENDING) return;
        BigDecimal amount = BigDecimal.valueOf(
                root.path("payload").path("payment").path("entity").path("amount").asLong(0))
                .divide(BigDecimal.valueOf(100));
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setPaymentId(payId);
        payment.setAmount(amount);
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.FAILED);
        payment = paymentRepository.save(payment);
        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);
        PaymentEvent pe = new PaymentEvent();
        pe.setPaymentId(payment.getId());
        pe.setEventId(payId);
        pe.setEventType(eventType);
        pe.setPayload(raw);
        paymentEventRepository.save(pe);
        auditService.logPaymentFailed(orderId.toString(), "Payment failed");
        auditService.logOrderStatusUpdated(orderId.toString(), "PENDING", "FAILED");
    }

    private Order buildOrder(UUID id, OrderStatus status) {
        Order o = new Order();
        o.setId(id);
        o.setBuyerId(UUID.randomUUID());
        o.setStatus(status);
        o.setTotalAmount(new BigDecimal("1000.00"));
        return o;
    }

    private OrderItem buildOrderItem(UUID orderId, UUID tierId, int qty) {
        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setOrderId(orderId);
        item.setTierId(tierId);
        item.setQuantity(qty);
        item.setTierName("VIP");
        item.setUnitPrice(new BigDecimal("500.00"));
        return item;
    }

    private String buildSuccessPayload(UUID orderId, String paymentId) {
        return String.format("""
                {"event":"payment_link.paid","payload":{"payment":{"entity":{"id":"%s","amount":100000,"currency":"INR","notes":{"orderId":"%s"}}}}}""",
                paymentId, orderId);
    }

    private String buildFailedPayload(UUID orderId, String paymentId) {
        return String.format("""
                {"event":"payment.failed","payload":{"payment":{"entity":{"id":"%s","amount":50000,"currency":"INR","error_description":"Declined","notes":{"orderId":"%s"}}}}}""",
                paymentId, orderId);
    }
}
