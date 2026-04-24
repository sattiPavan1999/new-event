package com.eventplatform.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditServiceTest {

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService();
    }

    @Test
    void testLogRegistrationSuccess() {
        assertDoesNotThrow(() -> auditService.logRegistration("test@example.com", "BUYER", true));
    }

    @Test
    void testLogRegistrationFailure() {
        assertDoesNotThrow(() -> auditService.logRegistration("test@example.com", "BUYER", false));
    }

    @Test
    void testLogLoginSuccess() {
        assertDoesNotThrow(() -> auditService.logLogin("test@example.com", true));
    }

    @Test
    void testLogLoginFailure() {
        assertDoesNotThrow(() -> auditService.logLogin("test@example.com", false));
    }

    @Test
    void testLogTokenRefreshSuccess() {
        assertDoesNotThrow(() -> auditService.logTokenRefresh("user-id-123", true));
    }

    @Test
    void testLogTokenRefreshFailure() {
        assertDoesNotThrow(() -> auditService.logTokenRefresh("user-id-123", false));
    }

    @Test
    void testLogLogoutSuccess() {
        assertDoesNotThrow(() -> auditService.logLogout("user-id-123", true));
    }

    @Test
    void testLogLogoutFailure() {
        assertDoesNotThrow(() -> auditService.logLogout("user-id-123", false));
    }

    @Test
    void testEmailMasking() {
        assertDoesNotThrow(() -> auditService.logRegistration("a@example.com", "BUYER", true));
        assertDoesNotThrow(() -> auditService.logRegistration("ab@example.com", "BUYER", true));
        assertDoesNotThrow(() -> auditService.logRegistration("abc@example.com", "BUYER", true));
    }

    @Test
    void testNullEmail() {
        assertDoesNotThrow(() -> auditService.logRegistration(null, "BUYER", true));
        assertDoesNotThrow(() -> auditService.logLogin(null, true));
    }

    @Test
    void testEmptyEmail() {
        assertDoesNotThrow(() -> auditService.logRegistration("", "BUYER", true));
        assertDoesNotThrow(() -> auditService.logLogin("", true));
    }

    @Test
    void testNullUserId() {
        assertDoesNotThrow(() -> auditService.logTokenRefresh(null, true));
        assertDoesNotThrow(() -> auditService.logLogout(null, true));
    }

    @Test
    void testShortUserId() {
        assertDoesNotThrow(() -> auditService.logTokenRefresh("123", true));
        assertDoesNotThrow(() -> auditService.logLogout("1234567", true));
    }
}
