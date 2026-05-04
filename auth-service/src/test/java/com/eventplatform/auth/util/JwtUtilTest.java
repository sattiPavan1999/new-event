package com.eventplatform.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "mySecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForHS256Algorithm";
    private final long accessTokenExpiry = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(secret, accessTokenExpiry);
    }

    @Test
    void testGenerateAccessToken() {
        Long userId = 42L;
        String email = "test@example.com";
        String role = "BUYER";

        String token = jwtUtil.generateAccessToken(userId, email, role);

        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(token.contains("."));
    }

    @Test
    void testValidateAccessToken() {
        Long userId = 42L;
        String email = "test@example.com";
        String role = "BUYER";

        String token = jwtUtil.generateAccessToken(userId, email, role);
        Claims claims = jwtUtil.validateToken(token);

        assertNotNull(claims);
        assertEquals(String.valueOf(userId), claims.getSubject());
        assertEquals(email, claims.get("email"));
        assertEquals(role, claims.get("role"));
        assertEquals("access", claims.get("type"));
    }

    @Test
    void testAccessTokenExpiry() throws InterruptedException {
        JwtUtil shortExpiryUtil = new JwtUtil(secret, 100L);
        Long userId = 1L;
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
    void testAccessTokenContainsClaims() {
        Long userId = 99L;
        String email = "buyer@example.com";
        String role = "ORGANISER";

        String token = jwtUtil.generateAccessToken(userId, email, role);
        Claims claims = jwtUtil.validateToken(token);

        assertEquals(String.valueOf(userId), claims.getSubject());
        assertEquals(email, claims.get("email"));
        assertEquals(role, claims.get("role"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }
}
