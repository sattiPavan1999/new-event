package com.eventplatform.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "mySecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForHS256Algorithm";
    private final long accessTokenExpiry = 900000L; // 15 minutes
    private final long refreshTokenExpiry = 604800000L; // 7 days

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(secret, accessTokenExpiry, refreshTokenExpiry);
    }

    @Test
    void testGenerateAccessToken() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String role = "BUYER";

        String token = jwtUtil.generateAccessToken(userId, email, role);

        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(token.contains("."));
    }

    @Test
    void testGenerateRefreshToken() {
        UUID userId = UUID.randomUUID();

        String token = jwtUtil.generateRefreshToken(userId);

        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(token.contains("."));
    }

    @Test
    void testValidateAccessToken() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String role = "BUYER";

        String token = jwtUtil.generateAccessToken(userId, email, role);
        Claims claims = jwtUtil.validateToken(token);

        assertNotNull(claims);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals(email, claims.get("email"));
        assertEquals(role, claims.get("role"));
        assertEquals("access", claims.get("type"));
    }

    @Test
    void testValidateRefreshToken() {
        UUID userId = UUID.randomUUID();

        String token = jwtUtil.generateRefreshToken(userId);
        Claims claims = jwtUtil.validateToken(token);

        assertNotNull(claims);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("refresh", claims.get("type"));
    }

    @Test
    void testGetUserIdFromToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateRefreshToken(userId);

        UUID extractedId = jwtUtil.getUserIdFromToken(token);

        assertEquals(userId, extractedId);
    }

    @Test
    void testGetRefreshTokenExpiryMillis() {
        long expiry = jwtUtil.getRefreshTokenExpiryMillis();
        assertEquals(refreshTokenExpiry, expiry);
    }

    @Test
    void testAccessTokenExpiry() throws InterruptedException {
        JwtUtil shortExpiryUtil = new JwtUtil(secret, 100L, refreshTokenExpiry);
        UUID userId = UUID.randomUUID();
        String token = shortExpiryUtil.generateAccessToken(userId, "test@example.com", "BUYER");

        Thread.sleep(150);

        assertThrows(ExpiredJwtException.class, () -> shortExpiryUtil.validateToken(token));
    }

    @Test
    void testInvalidToken() {
        String invalidToken = "invalid.jwt.token";
        assertThrows(Exception.class, () -> jwtUtil.validateToken(invalidToken));
    }

    @Test
    void testTokenWithDifferentUserIds() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        String token1 = jwtUtil.generateRefreshToken(userId1);
        String token2 = jwtUtil.generateRefreshToken(userId2);

        UUID extracted1 = jwtUtil.getUserIdFromToken(token1);
        UUID extracted2 = jwtUtil.getUserIdFromToken(token2);

        assertEquals(userId1, extracted1);
        assertEquals(userId2, extracted2);
        assertNotEquals(extracted1, extracted2);
    }

    @Test
    void testAccessTokenContainsClaims() {
        UUID userId = UUID.randomUUID();
        String email = "buyer@example.com";
        String role = "ORGANISER";

        String token = jwtUtil.generateAccessToken(userId, email, role);
        Claims claims = jwtUtil.validateToken(token);

        assertEquals(userId.toString(), claims.getSubject());
        assertEquals(email, claims.get("email"));
        assertEquals(role, claims.get("role"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void testRefreshTokenType() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateRefreshToken(userId);
        Claims claims = jwtUtil.validateToken(token);

        assertEquals("refresh", claims.get("type"));
    }
}
