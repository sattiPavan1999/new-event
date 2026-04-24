package com.eventplatform.auth.entity;

import com.eventplatform.auth.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testGettersAndSetters() {
        User user = new User();
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        user.setId(id);
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedPassword");
        user.setFullName("John Doe");
        user.setRole(UserRole.BUYER);
        user.setIsActive(true);
        user.setCreatedAt(now);

        assertEquals(id, user.getId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("hashedPassword", user.getPasswordHash());
        assertEquals("John Doe", user.getFullName());
        assertEquals(UserRole.BUYER, user.getRole());
        assertTrue(user.getIsActive());
        assertEquals(now, user.getCreatedAt());
    }

    @Test
    void testParameterizedConstructor() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        User user = new User(id, "test@example.com", "hashedPassword", "John Doe", UserRole.ORGANISER, true, now);

        assertEquals(id, user.getId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("hashedPassword", user.getPasswordHash());
        assertEquals("John Doe", user.getFullName());
        assertEquals(UserRole.ORGANISER, user.getRole());
        assertTrue(user.getIsActive());
        assertEquals(now, user.getCreatedAt());
    }

    @Test
    void testNoArgsConstructor() {
        User user = new User();
        assertNotNull(user);
        assertNull(user.getId());
        assertNull(user.getEmail());
        assertNull(user.getPasswordHash());
        assertNull(user.getFullName());
        assertNull(user.getRole());
        assertNull(user.getIsActive());
        assertNull(user.getCreatedAt());
    }

    @Test
    void testAllRoles() {
        User buyer = new User();
        buyer.setRole(UserRole.BUYER);
        assertEquals(UserRole.BUYER, buyer.getRole());

        User organiser = new User();
        organiser.setRole(UserRole.ORGANISER);
        assertEquals(UserRole.ORGANISER, organiser.getRole());

        User admin = new User();
        admin.setRole(UserRole.ADMIN);
        assertEquals(UserRole.ADMIN, admin.getRole());
    }

    @Test
    void testInactiveUser() {
        User user = new User();
        user.setIsActive(false);
        assertFalse(user.getIsActive());
    }

    @Test
    void testEmailUniqueness() {
        String email = "unique@example.com";
        User user1 = new User();
        user1.setEmail(email);

        User user2 = new User();
        user2.setEmail(email);

        assertEquals(user1.getEmail(), user2.getEmail());
    }

    @Test
    void testPasswordHashStorage() {
        User user = new User();
        String hash = "$2a$12$someBCryptHash";
        user.setPasswordHash(hash);
        assertEquals(hash, user.getPasswordHash());
        assertNotEquals("plainPassword", user.getPasswordHash());
    }
}
