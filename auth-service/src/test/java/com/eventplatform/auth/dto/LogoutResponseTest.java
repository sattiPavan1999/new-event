package com.eventplatform.auth.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogoutResponseTest {

    @Test
    void testGettersAndSetters() {
        LogoutResponse response = new LogoutResponse();
        response.setMessage("Logged out successfully");

        assertEquals("Logged out successfully", response.getMessage());
    }

    @Test
    void testParameterizedConstructor() {
        LogoutResponse response = new LogoutResponse("Logged out successfully");
        assertEquals("Logged out successfully", response.getMessage());
    }

    @Test
    void testNoArgsConstructor() {
        LogoutResponse response = new LogoutResponse();
        assertNotNull(response);
        assertNull(response.getMessage());
    }
}
