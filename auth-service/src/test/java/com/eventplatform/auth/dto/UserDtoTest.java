package com.eventplatform.auth.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserDtoTest {

    @Test
    void testGettersAndSetters() {
        UserDto dto = new UserDto();
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        dto.setId(id);
        dto.setEmail("test@example.com");
        dto.setFullName("John Doe");
        dto.setRole("BUYER");
        dto.setIsActive(true);
        dto.setCreatedAt(now);

        assertEquals(id, dto.getId());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("John Doe", dto.getFullName());
        assertEquals("BUYER", dto.getRole());
        assertTrue(dto.getIsActive());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    void testParameterizedConstructor() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        UserDto dto = new UserDto(id, "test@example.com", "John Doe", "ORGANISER", true, now);

        assertEquals(id, dto.getId());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("John Doe", dto.getFullName());
        assertEquals("ORGANISER", dto.getRole());
        assertTrue(dto.getIsActive());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    void testNoArgsConstructor() {
        UserDto dto = new UserDto();
        assertNotNull(dto);
        assertNull(dto.getId());
        assertNull(dto.getEmail());
        assertNull(dto.getFullName());
        assertNull(dto.getRole());
        assertNull(dto.getIsActive());
        assertNull(dto.getCreatedAt());
    }

    @Test
    void testInactiveUser() {
        UserDto dto = new UserDto();
        dto.setIsActive(false);
        assertFalse(dto.getIsActive());
    }
}
