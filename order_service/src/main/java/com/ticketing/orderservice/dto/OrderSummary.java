package com.ticketing.orderservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderSummary {

    private Long orderId;
    private String status;
    private BigDecimal totalAmount;
    private Instant createdAt;
    private List<OrderItemSummary> items;

    public OrderSummary() {
    }

    public OrderSummary(Long orderId, String status, BigDecimal totalAmount, Instant createdAt, List<OrderItemSummary> items) {
        this.orderId = orderId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.items = items;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderItemSummary> getItems() {
        return items;
    }

    public void setItems(List<OrderItemSummary> items) {
        this.items = items;
    }
}
