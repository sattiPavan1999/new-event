package com.eventplatform.auth.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void testGettersAndSetters() {
        ErrorResponse response = new ErrorResponse();
        LocalDateTime now = LocalDateTime.now();

        response.setErrorCode("TEST_ERROR");
        response.setMessage("Test error message");
        response.setTimestamp(now);
        response.setTraceId("trace123");

        assertEquals("TEST_ERROR", response.getErrorCode());
        assertEquals("Test error message", response.getMessage());
        assertEquals(now, response.getTimestamp());
        assertEquals("trace123", response.getTraceId());
    }

    @Test
    void testParameterizedConstructor() {
        ErrorResponse response = new ErrorResponse("TEST_ERROR", "Test error message", "trace123");

        assertEquals("TEST_ERROR", response.getErrorCode());
        assertEquals("Test error message", response.getMessage());
        assertNotNull(response.getTimestamp());
        assertEquals("trace123", response.getTraceId());
    }

    @Test
    void testNoArgsConstructor() {
        ErrorResponse response = new ErrorResponse();
        assertNotNull(response);
        assertNotNull(response.getTimestamp());
        assertNull(response.getErrorCode());
        assertNull(response.getMessage());
        assertNull(response.getTraceId());
    }

    @Test
    void testTimestampAutoGeneration() {
        ErrorResponse response = new ErrorResponse();
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertTrue(response.getTimestamp().isAfter(before) && response.getTimestamp().isBefore(after));
    }
}
