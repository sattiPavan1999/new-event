package com.ticketing.orderservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class CancelOrderResponse {

    private UUID orderId;
    private String status;
    private String message;
    private BigDecimal remainingBalance;

    public CancelOrderResponse() {
    }

    public CancelOrderResponse(UUID orderId, String status, String message, BigDecimal remainingBalance) {
        this.orderId = orderId;
        this.status = status;
        this.message = message;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }
}
