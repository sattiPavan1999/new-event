package com.eventmanagement.util;

import com.eventmanagement.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Long extractOrganiserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }
        try {
            String token = authHeader.substring(7);
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String role = claims.get("role", String.class);
            if (!"ORGANISER".equals(role)) {
                throw new UnauthorizedException("Access denied: ORGANISER role required");
            }
            return Long.parseLong(claims.getSubject());
        } catch (JwtException e) {
            throw new UnauthorizedException("Invalid or expired token");
        }
    }
}
