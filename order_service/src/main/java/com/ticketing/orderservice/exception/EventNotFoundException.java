package com.ticketing.orderservice.exception;

public class EventNotFoundException extends RuntimeException {

    private final Long eventId;

    public EventNotFoundException(Long eventId) {
        super("Event not found with id: " + eventId);
        this.eventId = eventId;
    }

    public Long getEventId() {
        return eventId;
    }
}
