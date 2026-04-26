package com.ticketing.orderservice.controller;

import com.ticketing.orderservice.dto.WebhookResponse;
import com.ticketing.orderservice.service.PaymentWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final PaymentWebhookService paymentWebhookService;

    public PaymentWebhookController(PaymentWebhookService paymentWebhookService) {
        this.paymentWebhookService = paymentWebhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<WebhookResponse> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String sigHeader) {

        try {
            paymentWebhookService.processWebhook(payload);
            return ResponseEntity.ok(new WebhookResponse(true));
        } catch (Exception e) {
            logger.error("Error processing webhook", e);
            return ResponseEntity.ok(new WebhookResponse(true));
        }
    }
}
