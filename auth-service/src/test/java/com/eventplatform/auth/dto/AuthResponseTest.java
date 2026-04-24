package com.eventplatform.auth.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthResponseTest {

    @Test
    void testGettersAndSetters() {
        AuthResponse response = new AuthResponse();
        UserDto userDto = new UserDto(UUID.randomUUID(), "test@example.com", "John Doe", "BUYER", true, LocalDateTime.now());

        response.setAccessToken("access123");
        response.setRefreshToken("refresh123");
        response.setUser(userDto);

        assertEquals("access123", response.getAccessToken());
        assertEquals("refresh123", response.getRefreshToken());
        assertEquals(userDto, response.getUser());
    }

    @Test
    void testParameterizedConstructor() {
        UserDto userDto = new UserDto(UUID.randomUUID(), "test@example.com", "John Doe", "BUYER", true, LocalDateTime.now());
        AuthResponse response = new AuthResponse("access123", "refresh123", userDto);

        assertEquals("access123", response.getAccessToken());
        assertEquals("refresh123", response.getRefreshToken());
        assertEquals(userDto, response.getUser());
    }

    @Test
    void testNoArgsConstructor() {
        AuthResponse response = new AuthResponse();
        assertNotNull(response);
        assertNull(response.getAccessToken());
        assertNull(response.getRefreshToken());
        assertNull(response.getUser());
    }
}
