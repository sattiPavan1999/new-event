package com.eventplatform.auth.repository;

import com.eventplatform.auth.entity.RefreshToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshToken saveToken(String tokenValue, LocalDateTime expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID());
        token.setUserId(UUID.randomUUID());
        token.setToken(tokenValue);
        token.setExpiresAt(expiresAt);
        token.setCreatedAt(LocalDateTime.now());
        return refreshTokenRepository.save(token);
    }

    @Test
    void findByToken_existingToken_returnsToken() {
        saveToken("valid-token-abc", LocalDateTime.now().plusDays(7));

        Optional<RefreshToken> result = refreshTokenRepository.findByToken("valid-token-abc");

        assertTrue(result.isPresent());
        assertEquals("valid-token-abc", result.get().getToken());
    }

    @Test
    void findByToken_nonExistingToken_returnsEmpty() {
        assertFalse(refreshTokenRepository.findByToken("nonexistent").isPresent());
    }

    @Test
    void deleteByToken_removesToken() {
        saveToken("to-delete", LocalDateTime.now().plusDays(7));

        refreshTokenRepository.deleteByToken("to-delete");

        assertFalse(refreshTokenRepository.findByToken("to-delete").isPresent());
    }

    @Test
    void deleteByToken_nonExistentToken_doesNotThrow() {
        assertDoesNotThrow(() -> refreshTokenRepository.deleteByToken("no-such-token"));
    }

    @Test
    void deleteByExpiresAtBefore_removesExpiredTokens() {
        saveToken("expired-1", LocalDateTime.now().minusDays(2));
        saveToken("expired-2", LocalDateTime.now().minusDays(1));
        saveToken("valid", LocalDateTime.now().plusDays(7));

        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        assertFalse(refreshTokenRepository.findByToken("expired-1").isPresent());
        assertFalse(refreshTokenRepository.findByToken("expired-2").isPresent());
        assertTrue(refreshTokenRepository.findByToken("valid").isPresent());
    }

    @Test
    void save_persistsAllFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime expires = LocalDateTime.now().plusDays(7);

        RefreshToken token = new RefreshToken(id, userId, "my-jwt-token", expires, LocalDateTime.now());
        refreshTokenRepository.save(token);

        Optional<RefreshToken> found = refreshTokenRepository.findById(id);
        assertTrue(found.isPresent());
        assertEquals(userId, found.get().getUserId());
    }
}
