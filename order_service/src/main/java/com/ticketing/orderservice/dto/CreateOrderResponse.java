package com.ticketing.orderservice.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CreateOrderResponse {

    private UUID orderId;
    private String status;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
    private BigDecimal remainingBalance;

    public CreateOrderResponse() {
    }

    public CreateOrderResponse(UUID orderId, String status, BigDecimal totalAmount, List<OrderItemResponse> items) {
        this.orderId = orderId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.items = items;
    }

    public CreateOrderResponse(UUID orderId, String status, BigDecimal totalAmount, List<OrderItemResponse> items, BigDecimal remainingBalance) {
        this.orderId = orderId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.items = items;
        this.remainingBalance = remainingBalance;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }
}
