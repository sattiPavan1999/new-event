package com.ticketing.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.payment.dto.CreateOrderRequest;
import com.ticketing.payment.dto.CreateOrderResponse;
import com.ticketing.payment.exception.InsufficientInventoryException;
import com.ticketing.payment.exception.InvalidTierException;
import com.ticketing.payment.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class PaymentOrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private OrderService orderService;

    @Test
    void createOrder_validRequest_returnsCreated() throws Exception {
        UUID orderId = UUID.randomUUID();
        CreateOrderResponse response = new CreateOrderResponse(orderId, "https://rzp.io/l/abc123");
        when(orderService.createOrder(any(), any())).thenReturn(response);

        CreateOrderRequest request = new CreateOrderRequest(UUID.randomUUID(), 2);

        mockMvc.perform(post("/api/orders")
                        .header("X-Buyer-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.checkoutUrl").value("https://rzp.io/l/abc123"));
    }

    @Test
    void createOrder_invalidTier_returnsBadRequest() throws Exception {
        when(orderService.createOrder(any(), any()))
                .thenThrow(new InvalidTierException("Tier not found"));

        CreateOrderRequest request = new CreateOrderRequest(UUID.randomUUID(), 2);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TIER"));
    }

    @Test
    void createOrder_insufficientInventory_returnsBadRequest() throws Exception {
        when(orderService.createOrder(any(), any()))
                .thenThrow(new InsufficientInventoryException("Not enough stock"));

        CreateOrderRequest request = new CreateOrderRequest(UUID.randomUUID(), 10);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_INVENTORY"));
    }

    @Test
    void createOrder_missingTierId_returnsBadRequest() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(null, 2);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
