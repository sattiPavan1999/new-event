package com.ticketing.orderservice.exception;

public class TierNotFoundException extends RuntimeException {

    private final Long tierId;

    public TierNotFoundException(Long tierId) {
        super("Ticket tier not found with id: " + tierId);
        this.tierId = tierId;
    }

    public Long getTierId() {
        return tierId;
    }
}
