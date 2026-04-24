package com.eventmanagement.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TierStatusTest {

    @Test
    void testTierStatusValues() {
        assertEquals(3, TierStatus.values().length);
        assertNotNull(TierStatus.valueOf("ACTIVE"));
        assertNotNull(TierStatus.valueOf("CLOSED"));
        assertNotNull(TierStatus.valueOf("SOLD_OUT"));
    }

    @Test
    void testTierStatusActive() {
        assertEquals("ACTIVE", TierStatus.ACTIVE.name());
    }

    @Test
    void testTierStatusClosed() {
        assertEquals("CLOSED", TierStatus.CLOSED.name());
    }

    @Test
    void testTierStatusSoldOut() {
        assertEquals("SOLD_OUT", TierStatus.SOLD_OUT.name());
    }
}
