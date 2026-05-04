package com.ticketing.orderservice.exception;

public class OrderAccessDeniedException extends RuntimeException {

    private final Long orderId;

    public OrderAccessDeniedException(Long orderId) {
        super("Access denied to order with id: " + orderId);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
