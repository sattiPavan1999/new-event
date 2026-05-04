package com.ticketing.orderservice.exception;

public class OrderCancellationNotAllowedException extends RuntimeException {

    private final Long orderId;

    public OrderCancellationNotAllowedException(Long orderId, String reason) {
        super("Cannot cancel order " + orderId + ": " + reason);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
