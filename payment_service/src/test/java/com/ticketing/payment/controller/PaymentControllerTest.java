package com.ticketing.payment.controller;

import com.ticketing.payment.exception.DuplicateEventException;
import com.ticketing.payment.exception.InvalidWebhookSignatureException;
import com.ticketing.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private PaymentService paymentService;

    private static final String VALID_PAYLOAD = "{\"event\":\"payment_link.paid\"}";

    @Test
    void handleWebhook_validSignature_returnsOk() throws Exception {
        doNothing().when(paymentService).processWebhook(any(), any());

        mockMvc.perform(post("/api/payments/webhook")
                        .header("X-Razorpay-Signature", "valid-sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));
    }

    @Test
    void handleWebhook_invalidSignature_returnsUnauthorized() throws Exception {
        doThrow(new InvalidWebhookSignatureException("Invalid signature"))
                .when(paymentService).processWebhook(any(), any());

        mockMvc.perform(post("/api/payments/webhook")
                        .header("X-Razorpay-Signature", "bad-sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void handleWebhook_duplicateEvent_returnsOkAnyway() throws Exception {
        doThrow(new DuplicateEventException("Already processed"))
                .when(paymentService).processWebhook(any(), any());

        mockMvc.perform(post("/api/payments/webhook")
                        .header("X-Razorpay-Signature", "sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));
    }
}
