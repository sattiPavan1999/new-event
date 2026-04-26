package com.ticketing.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.payment.entity.*;
import com.ticketing.payment.exception.DuplicateEventException;
import com.ticketing.payment.exception.InvalidWebhookSignatureException;
import com.ticketing.payment.exception.OrderNotFoundException;
import com.ticketing.payment.repository.OrderItemRepository;
import com.ticketing.payment.repository.OrderRepository;
import com.ticketing.payment.repository.PaymentEventRepository;
import com.ticketing.payment.repository.PaymentRepository;
import com.ticketing.payment.repository.TicketTierRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TicketTierRepository ticketTierRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Value("${webhook.secret:}")
    private String webhookSecret;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentEventRepository paymentEventRepository,
                          OrderRepository orderRepository,
                          OrderItemRepository orderItemRepository,
                          TicketTierRepository ticketTierRepository,
                          AuditService auditService,
                          ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.ticketTierRepository = ticketTierRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void processWebhook(String payload, String signatureHeader) {
        verifyWebhookSignature(payload, signatureHeader);

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse webhook payload", e);
        }

        String eventType = root.path("event").asText();
        String paymentId = root.path("payload").path("payment").path("entity").path("id").asText("");

        auditService.logWebhookReceived(paymentId, eventType);

        if (paymentEventRepository.existsByEventId(paymentId)) {
            auditService.logDuplicateWebhook(paymentId);
            throw new DuplicateEventException("Event already processed: " + paymentId);
        }

        if ("payment_link.paid".equals(eventType)) {
            handlePaymentSuccess(root, paymentId, payload, eventType);
        } else if ("payment.failed".equals(eventType)) {
            handlePaymentFailure(root, paymentId, payload, eventType);
        }
    }

    private void verifyWebhookSignature(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            return;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            if (!computed.equals(signatureHeader)) {
                auditService.logWebhookSignatureFailure("Signature mismatch");
                throw new InvalidWebhookSignatureException("Invalid webhook signature");
            }
        } catch (InvalidWebhookSignatureException e) {
            throw e;
        } catch (Exception e) {
            auditService.logWebhookSignatureFailure(e.getMessage());
            throw new InvalidWebhookSignatureException("Webhook signature verification failed: " + e.getMessage());
        }
    }

    private void handlePaymentSuccess(JsonNode root, String paymentId, String rawPayload, String eventType) {
        JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
        JsonNode notes = paymentEntity.path("notes");

        UUID orderId = UUID.fromString(notes.path("orderId").asText());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            auditService.logOrderStatusUpdated(orderId.toString(), order.getStatus().name(), order.getStatus().name());
            return;
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        if (orderItems.isEmpty()) {
            throw new OrderNotFoundException("Order items not found for order: " + orderId);
        }

        OrderItem orderItem = orderItems.get(0);
        int rowsUpdated = ticketTierRepository.decrementInventory(orderItem.getTierId(), orderItem.getQuantity());

        long amountInPaise = paymentEntity.path("amount").asLong(0);
        BigDecimal amount = BigDecimal.valueOf(amountInPaise).divide(BigDecimal.valueOf(100));
        String currency = paymentEntity.path("currency").asText("INR").toUpperCase();

        if (rowsUpdated == 1) {
            Payment payment = new Payment();
            payment.setOrderId(orderId);
            payment.setPaymentId(paymentId);
            payment.setAmount(amount);
            payment.setCurrency(currency);
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment = paymentRepository.save(payment);

            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            PaymentEvent paymentEvent = new PaymentEvent();
            paymentEvent.setPaymentId(payment.getId());
            paymentEvent.setEventId(paymentId);
            paymentEvent.setEventType(eventType);
            paymentEvent.setPayload(rawPayload);
            paymentEventRepository.save(paymentEvent);

            auditService.logInventoryDecremented(orderItem.getTierId().toString(), orderItem.getQuantity());
            auditService.logPaymentSuccess(orderId.toString(), payment.getId().toString(), amount.toString());
            auditService.logOrderStatusUpdated(orderId.toString(), OrderStatus.PENDING.name(), OrderStatus.CONFIRMED.name());
        } else {
            auditService.logOversellDetected(orderId.toString(), orderItem.getTierId().toString());

            Payment payment = new Payment();
            payment.setOrderId(orderId);
            payment.setPaymentId(paymentId);
            payment.setAmount(amount);
            payment.setCurrency(currency);
            payment.setStatus(PaymentStatus.FAILED);
            payment = paymentRepository.save(payment);

            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);

            PaymentEvent paymentEvent = new PaymentEvent();
            paymentEvent.setPaymentId(payment.getId());
            paymentEvent.setEventId(paymentId);
            paymentEvent.setEventType(eventType);
            paymentEvent.setPayload(rawPayload);
            paymentEventRepository.save(paymentEvent);

            auditService.logPaymentFailed(orderId.toString(), "Concurrent oversell detected");
            auditService.logOrderStatusUpdated(orderId.toString(), OrderStatus.PENDING.name(), OrderStatus.FAILED.name());
        }
    }

    private void handlePaymentFailure(JsonNode root, String paymentId, String rawPayload, String eventType) {
        JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
        JsonNode notes = paymentEntity.path("notes");

        UUID orderId = UUID.fromString(notes.path("orderId").asText());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        long amountInPaise = paymentEntity.path("amount").asLong(0);
        BigDecimal amount = BigDecimal.valueOf(amountInPaise).divide(BigDecimal.valueOf(100));
        String currency = paymentEntity.path("currency").asText("INR").toUpperCase();
        String errorDescription = paymentEntity.path("error_description").asText("Unknown error");

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setPaymentId(paymentId);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(PaymentStatus.FAILED);
        payment = paymentRepository.save(payment);

        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);

        PaymentEvent paymentEvent = new PaymentEvent();
        paymentEvent.setPaymentId(payment.getId());
        paymentEvent.setEventId(paymentId);
        paymentEvent.setEventType(eventType);
        paymentEvent.setPayload(rawPayload);
        paymentEventRepository.save(paymentEvent);

        auditService.logPaymentFailed(orderId.toString(), "Payment failed: " + errorDescription);
        auditService.logOrderStatusUpdated(orderId.toString(), OrderStatus.PENDING.name(), OrderStatus.FAILED.name());
    }
}
