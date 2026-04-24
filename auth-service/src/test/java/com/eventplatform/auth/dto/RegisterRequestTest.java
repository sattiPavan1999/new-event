package com.eventplatform.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidRegisterRequest() {
        RegisterRequest request = new RegisterRequest("test@example.com", "John Doe", "Password123", "BUYER");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testNullEmail() {
        RegisterRequest request = new RegisterRequest(null, "John Doe", "Password123", "BUYER");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
    }

    @Test
    void testBlankEmail() {
        RegisterRequest request = new RegisterRequest("", "John Doe", "Password123", "BUYER");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testInvalidEmailFormat() {
        RegisterRequest request = new RegisterRequest("invalidemail", "John Doe", "Password123", "BUYER");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testNullFullName() {
        RegisterRequest request = new RegisterRequest("test@example.com", null, "Password123", "BUYER");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testBlankFullName() {
        RegisterRequest request = new RegisterRequest("test@example.com", "", "Password123", "BUYER");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testNullPassword() {
        RegisterRequest request = new RegisterRequest("test@example.com", "John Doe", null, "BUYER");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testBlankPassword() {
        RegisterRequest request = new RegisterRequest("test@example.com", "John Doe", "", "BUYER");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testShortPassword() {
        RegisterRequest request = new RegisterRequest("test@example.com", "John Doe", "Pass1", "BUYER");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testPasswordWithoutDigit() {
        RegisterRequest request = new RegisterRequest("test@example.com", "John Doe", "PasswordOnly", "BUYER");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testPasswordExactly8CharsWithDigit() {
        RegisterRequest request = new RegisterRequest("test@example.com", "John Doe", "Pass1234", "BUYER");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testNullRole() {
        RegisterRequest request = new RegisterRequest("test@example.com", "John Doe", "Password123", null);
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testBlankRole() {
        RegisterRequest request = new RegisterRequest("test@example.com", "John Doe", "Password123", "");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testGettersAndSetters() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setFullName("John Doe");
        request.setPassword("Password123");
        request.setRole("BUYER");

        assertEquals("test@example.com", request.getEmail());
        assertEquals("John Doe", request.getFullName());
        assertEquals("Password123", request.getPassword());
        assertEquals("BUYER", request.getRole());
    }

    @Test
    void testParameterizedConstructor() {
        RegisterRequest request = new RegisterRequest("test@example.com", "John Doe", "Password123", "ORGANISER");
        assertEquals("test@example.com", request.getEmail());
        assertEquals("John Doe", request.getFullName());
        assertEquals("Password123", request.getPassword());
        assertEquals("ORGANISER", request.getRole());
    }

    @Test
    void testNoArgsConstructor() {
        RegisterRequest request = new RegisterRequest();
        assertNotNull(request);
        assertNull(request.getEmail());
        assertNull(request.getFullName());
        assertNull(request.getPassword());
        assertNull(request.getRole());
    }
}
