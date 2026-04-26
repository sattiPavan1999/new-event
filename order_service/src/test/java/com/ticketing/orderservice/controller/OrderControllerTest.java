package com.ticketing.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.orderservice.dto.*;
import com.ticketing.orderservice.entity.OrderStatus;
import com.ticketing.orderservice.exception.UnauthorizedException;
import com.ticketing.orderservice.service.OrderService;
import com.ticketing.orderservice.util.AuditService;
import com.ticketing.orderservice.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
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
    @MockBean private OrderService orderService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private AuditService auditService;

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
}
