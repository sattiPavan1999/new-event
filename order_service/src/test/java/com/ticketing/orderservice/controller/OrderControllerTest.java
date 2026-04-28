package com.ticketing.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.orderservice.dto.*;
import com.ticketing.orderservice.exception.InsufficientWalletBalanceException;
import com.ticketing.orderservice.exception.OrderCancellationNotAllowedException;
import com.ticketing.orderservice.exception.UnauthorizedException;
import com.ticketing.orderservice.service.OrderService;
import com.ticketing.orderservice.service.AuditService;
import com.ticketing.orderservice.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private OrderService orderService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private AuditService auditService;

    private final UUID buyerId = UUID.randomUUID();
    private final String authHeader = "Bearer valid.jwt.token";

    @Test
    void createOrder_validRequest_returnsCreated() throws Exception {
        UUID orderId = UUID.randomUUID();
        CreateOrderResponse response = new CreateOrderResponse(
                orderId, "CONFIRMED", new BigDecimal("3000.00"),
                List.of(new OrderItemResponse(UUID.randomUUID(), 2, new BigDecimal("1500.00"))));

        when(jwtUtil.getBuyerIdFromToken("valid.jwt.token")).thenReturn(buyerId);
        when(orderService.createOrder(any(), eq(buyerId))).thenReturn(response);

        CreateOrderRequest request = new CreateOrderRequest(UUID.randomUUID(),
                List.of(new OrderItemRequest(UUID.randomUUID(), 2)));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()));
    }

    @Test
    void createOrder_missingAuthHeader_returnsUnauthorized() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(UUID.randomUUID(),
                List.of(new OrderItemRequest(UUID.randomUUID(), 1)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrder_invalidRole_returnsUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("BUYER role required"))
                .when(jwtUtil).validateBuyerRole("valid.jwt.token");

        CreateOrderRequest request = new CreateOrderRequest(UUID.randomUUID(),
                List.of(new OrderItemRequest(UUID.randomUUID(), 1)));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyOrders_validRequest_returnsOk() throws Exception {
        OrderHistoryResponse response = new OrderHistoryResponse(
                Collections.emptyList(), 0, 10, 0L, 0);
        when(jwtUtil.getBuyerIdFromToken("valid.jwt.token")).thenReturn(buyerId);
        when(orderService.getMyOrders(eq(buyerId), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/orders/my")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void getOrderById_validRequest_returnsOk() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderDetailResponse detail = new OrderDetailResponse(
                orderId, "CONFIRMED", new BigDecimal("3000.00"),
                null, Instant.now(), Instant.now(), Collections.emptyList());

        when(jwtUtil.getBuyerIdFromToken("valid.jwt.token")).thenReturn(buyerId);
        when(orderService.getOrderById(orderId, buyerId)).thenReturn(detail);

        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void getMyOrders_missingAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/orders/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelOrder_validRequest_returnsOk() throws Exception {
        UUID orderId = UUID.randomUUID();
        CancelOrderResponse response = new CancelOrderResponse(
                orderId, "CANCELLED", "Order cancelled. ₹3000.00 credited back to your wallet within 1 day.",
                new BigDecimal("7000.00"));

        when(jwtUtil.getBuyerIdFromToken("valid.jwt.token")).thenReturn(buyerId);
        when(orderService.cancelOrder(orderId, buyerId)).thenReturn(response);

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()));
    }

    @Test
    void cancelOrder_missingAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/orders/{id}/cancel", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelOrder_notAllowed_returnsConflict() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(jwtUtil.getBuyerIdFromToken("valid.jwt.token")).thenReturn(buyerId);
        when(orderService.cancelOrder(orderId, buyerId))
                .thenThrow(new OrderCancellationNotAllowedException(orderId, "only CONFIRMED orders can be cancelled"));

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", authHeader))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CANCELLATION_NOT_ALLOWED"));
    }

    @Test
    void cancelOrder_insufficientBalance_returnsPaymentRequired() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(jwtUtil.getBuyerIdFromToken("valid.jwt.token")).thenReturn(buyerId);
        when(orderService.cancelOrder(orderId, buyerId))
                .thenThrow(new InsufficientWalletBalanceException(new BigDecimal("500.00")));

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", authHeader))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_BALANCE"));
    }
}
