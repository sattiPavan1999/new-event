package com.ticketing.orderservice.dto;

import java.math.BigDecimal;

public class CancelOrderResponse {

    private Long orderId;
    private String status;
    private String message;
    private BigDecimal remainingBalance;

    public CancelOrderResponse() {
    }

    public CancelOrderResponse(Long orderId, String status, String message, BigDecimal remainingBalance) {
        this.orderId = orderId;
        this.status = status;
        this.message = message;
        this.remainingBalance = remainingBalance;
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
