package com.eventmanagement.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EventStatusTest {

    @Test
    void testEventStatusValues() {
        assertEquals(3, EventStatus.values().length);
        assertNotNull(EventStatus.valueOf("DRAFT"));
        assertNotNull(EventStatus.valueOf("PUBLISHED"));
        assertNotNull(EventStatus.valueOf("CANCELLED"));
    }

    @Test
    void testEventStatusDraft() {
        assertEquals("DRAFT", EventStatus.DRAFT.name());
    }

    @Test
    void testEventStatusPublished() {
        assertEquals("PUBLISHED", EventStatus.PUBLISHED.name());
    }

    @Test
    void testEventStatusCancelled() {
        assertEquals("CANCELLED", EventStatus.CANCELLED.name());
    }
}
