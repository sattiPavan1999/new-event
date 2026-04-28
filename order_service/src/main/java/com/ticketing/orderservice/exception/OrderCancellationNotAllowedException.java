package com.ticketing.orderservice.exception;

import java.util.UUID;

public class OrderCancellationNotAllowedException extends RuntimeException {

    private final UUID orderId;

    public OrderCancellationNotAllowedException(UUID orderId, String reason) {
        super("Cannot cancel order " + orderId + ": " + reason);
        this.orderId = orderId;
    }

    public UUID getOrderId() {
        return orderId;
    }
}
