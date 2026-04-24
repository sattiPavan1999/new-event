package com.eventmanagement.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EventCategoryTest {

    @Test
    void testEventCategoryValues() {
        assertEquals(4, EventCategory.values().length);
        assertNotNull(EventCategory.valueOf("CONCERT"));
        assertNotNull(EventCategory.valueOf("SPORTS"));
        assertNotNull(EventCategory.valueOf("CONFERENCE"));
        assertNotNull(EventCategory.valueOf("OTHER"));
    }

    @Test
    void testEventCategoryConcert() {
        assertEquals("CONCERT", EventCategory.CONCERT.name());
    }

    @Test
    void testEventCategorySports() {
        assertEquals("SPORTS", EventCategory.SPORTS.name());
    }

    @Test
    void testEventCategoryConference() {
        assertEquals("CONFERENCE", EventCategory.CONFERENCE.name());
    }

    @Test
    void testEventCategoryOther() {
        assertEquals("OTHER", EventCategory.OTHER.name());
    }
}
