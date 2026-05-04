package com.ticketing.orderservice.controller;

import com.ticketing.orderservice.dto.CancelOrderResponse;
import com.ticketing.orderservice.dto.CreateOrderRequest;
import com.ticketing.orderservice.dto.CreateOrderResponse;
import com.ticketing.orderservice.dto.OrderDetailResponse;
import com.ticketing.orderservice.dto.OrderHistoryResponse;
import com.ticketing.orderservice.service.OrderService;
import com.ticketing.orderservice.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;

    public OrderController(OrderService orderService, JwtUtil jwtUtil) {
        this.orderService = orderService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            HttpServletRequest httpRequest) {

        String token = resolveToken(authorizationHeader, httpRequest);
        jwtUtil.validateBuyerRole(token);
        Long buyerId = jwtUtil.getBuyerIdFromToken(token);

        CreateOrderResponse response = orderService.createOrder(request, buyerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<OrderHistoryResponse> getMyOrders(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            HttpServletRequest httpRequest) {

        String token = resolveToken(authorizationHeader, httpRequest);
        jwtUtil.validateBuyerRole(token);
        Long buyerId = jwtUtil.getBuyerIdFromToken(token);

        OrderHistoryResponse response = orderService.getMyOrders(buyerId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailResponse> getOrderById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            HttpServletRequest httpRequest) {

        String token = resolveToken(authorizationHeader, httpRequest);
        jwtUtil.validateBuyerRole(token);
        Long buyerId = jwtUtil.getBuyerIdFromToken(token);

        OrderDetailResponse response = orderService.getOrderById(id, buyerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<CancelOrderResponse> cancelOrder(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            HttpServletRequest httpRequest) {

        String token = resolveToken(authorizationHeader, httpRequest);
        jwtUtil.validateBuyerRole(token);
        Long buyerId = jwtUtil.getBuyerIdFromToken(token);

        CancelOrderResponse response = orderService.cancelOrder(id, buyerId);
        return ResponseEntity.ok(response);
    }

    private String resolveToken(String authorizationHeader, HttpServletRequest request) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        throw new com.ticketing.orderservice.exception.UnauthorizedException("Authentication required");
    }
}
