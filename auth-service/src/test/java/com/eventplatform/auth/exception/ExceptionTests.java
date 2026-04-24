package com.eventplatform.auth.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTests {

    @Test
    void testDuplicateEmailException() {
        String message = "Email already registered";
        DuplicateEmailException exception = new DuplicateEmailException(message);

        assertEquals(message, exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testInvalidCredentialsException() {
        String message = "Invalid email or password";
        InvalidCredentialsException exception = new InvalidCredentialsException(message);

        assertEquals(message, exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testInvalidTokenException() {
        String message = "Invalid or expired refresh token";
        InvalidTokenException exception = new InvalidTokenException(message);

        assertEquals(message, exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testInvalidRoleException() {
        String message = "Invalid role. Must be BUYER or ORGANISER";
        InvalidRoleException exception = new InvalidRoleException(message);

        assertEquals(message, exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testExceptionThrow() {
        assertThrows(DuplicateEmailException.class, () -> {
            throw new DuplicateEmailException("Test");
        });

        assertThrows(InvalidCredentialsException.class, () -> {
            throw new InvalidCredentialsException("Test");
        });

        assertThrows(InvalidTokenException.class, () -> {
            throw new InvalidTokenException("Test");
        });

        assertThrows(InvalidRoleException.class, () -> {
            throw new InvalidRoleException("Test");
        });
    }

    @Test
    void testExceptionInheritance() {
        DuplicateEmailException de = new DuplicateEmailException("test");
        InvalidCredentialsException ice = new InvalidCredentialsException("test");
        InvalidTokenException ite = new InvalidTokenException("test");
        InvalidRoleException ire = new InvalidRoleException("test");

        assertTrue(de instanceof RuntimeException);
        assertTrue(ice instanceof RuntimeException);
        assertTrue(ite instanceof RuntimeException);
        assertTrue(ire instanceof RuntimeException);
    }
}
