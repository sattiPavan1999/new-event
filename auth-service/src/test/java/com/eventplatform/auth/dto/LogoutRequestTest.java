package com.eventplatform.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LogoutRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidLogoutRequest() {
        LogoutRequest request = new LogoutRequest("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
        Set<ConstraintViolation<LogoutRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testNullRefreshToken() {
        LogoutRequest request = new LogoutRequest(null);
        Set<ConstraintViolation<LogoutRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testBlankRefreshToken() {
        LogoutRequest request = new LogoutRequest("");
        Set<ConstraintViolation<LogoutRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testGettersAndSetters() {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("token123");

        assertEquals("token123", request.getRefreshToken());
    }

    @Test
    void testParameterizedConstructor() {
        LogoutRequest request = new LogoutRequest("token456");
        assertEquals("token456", request.getRefreshToken());
    }

    @Test
    void testNoArgsConstructor() {
        LogoutRequest request = new LogoutRequest();
        assertNotNull(request);
        assertNull(request.getRefreshToken());
    }
}
