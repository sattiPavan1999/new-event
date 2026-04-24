package com.eventmanagement.exception;

import com.eventmanagement.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleResourceNotFoundException() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Event not found");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFound(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("RESOURCE_NOT_FOUND", response.getBody().getErrorCode());
        assertEquals("Event not found", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertNotNull(response.getBody().getTraceId());
    }

    @Test
    void testHandleBusinessRuleViolation() {
        BusinessRuleViolationException exception = new BusinessRuleViolationException("Maximum 10 tiers allowed");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessRuleViolation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("BUSINESS_RULE_VIOLATION", response.getBody().getErrorCode());
        assertEquals("Maximum 10 tiers allowed", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertNotNull(response.getBody().getTraceId());
    }

    @Test
    void testHandleUnauthorized() {
        UnauthorizedException exception = new UnauthorizedException("Authentication token required");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnauthorized(exception);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UNAUTHORIZED", response.getBody().getErrorCode());
        assertEquals("Authentication token required", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertNotNull(response.getBody().getTraceId());
    }

    @Test
    void testHandleForbidden() {
        ForbiddenException exception = new ForbiddenException("Insufficient permissions");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleForbidden(exception);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FORBIDDEN", response.getBody().getErrorCode());
        assertEquals("Insufficient permissions", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertNotNull(response.getBody().getTraceId());
    }

    @Test
    void testHandleGenericException() {
        Exception exception = new Exception("Unexpected error occurred");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getErrorCode());
        assertTrue(response.getBody().getMessage().contains("unexpected error occurred"));
        assertNotNull(response.getBody().getTimestamp());
        assertNotNull(response.getBody().getTraceId());
    }
}
