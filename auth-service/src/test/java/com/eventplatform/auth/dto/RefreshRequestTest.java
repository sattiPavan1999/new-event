package com.eventplatform.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RefreshRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidRefreshRequest() {
        RefreshRequest request = new RefreshRequest("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
        Set<ConstraintViolation<RefreshRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testNullRefreshToken() {
        RefreshRequest request = new RefreshRequest(null);
        Set<ConstraintViolation<RefreshRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testBlankRefreshToken() {
        RefreshRequest request = new RefreshRequest("");
        Set<ConstraintViolation<RefreshRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testGettersAndSetters() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("token123");

        assertEquals("token123", request.getRefreshToken());
    }

    @Test
    void testParameterizedConstructor() {
        RefreshRequest request = new RefreshRequest("token456");
        assertEquals("token456", request.getRefreshToken());
    }

    @Test
    void testNoArgsConstructor() {
        RefreshRequest request = new RefreshRequest();
        assertNotNull(request);
        assertNull(request.getRefreshToken());
    }
}
