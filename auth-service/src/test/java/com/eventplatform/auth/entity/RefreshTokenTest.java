package com.eventplatform.auth.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenTest {

    @Test
    void testGettersAndSetters() {
        RefreshToken token = new RefreshToken();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = now.plusDays(7);

        token.setId(id);
        token.setUserId(userId);
        token.setToken("jwt.token.here");
        token.setExpiresAt(expires);
        token.setCreatedAt(now);

        assertEquals(id, token.getId());
        assertEquals(userId, token.getUserId());
        assertEquals("jwt.token.here", token.getToken());
        assertEquals(expires, token.getExpiresAt());
        assertEquals(now, token.getCreatedAt());
    }

    @Test
    void testParameterizedConstructor() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = now.plusDays(7);

        RefreshToken token = new RefreshToken(id, userId, "jwt.token.here", expires, now);

        assertEquals(id, token.getId());
        assertEquals(userId, token.getUserId());
        assertEquals("jwt.token.here", token.getToken());
        assertEquals(expires, token.getExpiresAt());
        assertEquals(now, token.getCreatedAt());
    }

    @Test
    void testNoArgsConstructor() {
        RefreshToken token = new RefreshToken();
        assertNotNull(token);
        assertNull(token.getId());
        assertNull(token.getUserId());
        assertNull(token.getToken());
        assertNull(token.getExpiresAt());
        assertNull(token.getCreatedAt());
    }

    @Test
    void testExpiryValidation() {
        RefreshToken token = new RefreshToken();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusDays(7);
        LocalDateTime past = now.minusDays(1);

        token.setCreatedAt(now);
        token.setExpiresAt(future);

        assertTrue(token.getExpiresAt().isAfter(token.getCreatedAt()));

        token.setExpiresAt(past);
        assertTrue(token.getExpiresAt().isBefore(now));
    }

    @Test
    void testTokenUniqueness() {
        String tokenValue = "unique.jwt.token.123";
        RefreshToken token1 = new RefreshToken();
        token1.setToken(tokenValue);

        RefreshToken token2 = new RefreshToken();
        token2.setToken(tokenValue);

        assertEquals(token1.getToken(), token2.getToken());
    }

    @Test
    void testSevenDayExpiry() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysLater = now.plusDays(7);

        RefreshToken token = new RefreshToken();
        token.setCreatedAt(now);
        token.setExpiresAt(sevenDaysLater);

        long daysBetween = java.time.Duration.between(token.getCreatedAt(), token.getExpiresAt()).toDays();
        assertEquals(7, daysBetween);
    }
}
