package com.eventplatform.auth.service;

import com.eventplatform.auth.dto.*;
import com.eventplatform.auth.entity.RefreshToken;
import com.eventplatform.auth.entity.User;
import com.eventplatform.auth.enums.UserRole;
import com.eventplatform.auth.exception.*;
import com.eventplatform.auth.repository.RefreshTokenRepository;
import com.eventplatform.auth.repository.UserRepository;
import com.eventplatform.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuditService auditService;
    @Mock private EntityManager entityManager;

    private AuthService authService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, jwtUtil, auditService, entityManager);
    }

    // ── Register ──────────────────────────────────────────────────────────────

    @Test
    void register_success_returnTokensAndUser() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(jwtUtil.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtUtil.getRefreshTokenExpiryMillis()).thenReturn(604800000L);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(
                new RegisterRequest("new@example.com", "New User", "Password123", "BUYER"));

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("new@example.com", response.getUser().getEmail());
        assertEquals("BUYER", response.getUser().getRole());
        assertTrue(response.getUser().getIsActive());
    }

    @Test
    void register_duplicateEmail_throwsDuplicateEmailException() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class,
                () -> authService.register(new RegisterRequest("dup@example.com", "User", "Password123", "BUYER")));
    }

    @Test
    void register_invalidRole_throwsInvalidRoleException() {
        when(userRepository.existsByEmail(any())).thenReturn(false);

        assertThrows(InvalidRoleException.class,
                () -> authService.register(new RegisterRequest("a@b.com", "User", "Password123", "INVALID_ROLE")));
    }

    @Test
    void register_adminRole_throwsInvalidRoleException() {
        when(userRepository.existsByEmail(any())).thenReturn(false);

        assertThrows(InvalidRoleException.class,
                () -> authService.register(new RegisterRequest("a@b.com", "User", "Password123", "ADMIN")));
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void login_success_returnsTokens() {
        User user = buildUser("login@example.com", encoder.encode("Password123"), UserRole.BUYER);
        when(userRepository.findByEmail("login@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(any(), any(), any())).thenReturn("access");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtUtil.getRefreshTokenExpiryMillis()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.login(new LoginRequest("login@example.com", "Password123"));

        assertNotNull(response);
        assertEquals("login@example.com", response.getUser().getEmail());
    }

    @Test
    void login_emailNotFound_throwsInvalidCredentialsException() {
        when(userRepository.findByEmail("no@user.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("no@user.com", "Password123")));
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentialsException() {
        User user = buildUser("pw@example.com", encoder.encode("CorrectPassword1"), UserRole.BUYER);
        when(userRepository.findByEmail("pw@example.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("pw@example.com", "WrongPassword1")));
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_validToken_returnsNewTokens() {
        UUID userId = UUID.randomUUID();
        User user = buildUser("r@example.com", "hash", UserRole.BUYER);
        user.setId(userId);

        RefreshToken stored = new RefreshToken();
        stored.setToken("old-token");
        stored.setUserId(userId);
        stored.setExpiresAt(LocalDateTime.now().plusDays(7));

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(userId.toString());

        when(refreshTokenRepository.findByToken("old-token")).thenReturn(Optional.of(stored));
        when(jwtUtil.validateToken("old-token")).thenReturn(claims);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(any(), any(), any())).thenReturn("new-access");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("new-refresh");
        when(jwtUtil.getRefreshTokenExpiryMillis()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.refresh(new RefreshRequest("old-token"));

        assertNotNull(response);
        assertEquals("new-access", response.getAccessToken());
        verify(refreshTokenRepository).delete(stored);
    }

    @Test
    void refresh_tokenNotFound_throwsInvalidTokenException() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class,
                () -> authService.refresh(new RefreshRequest("missing")));
    }

    @Test
    void refresh_expiredToken_throwsInvalidTokenException() {
        RefreshToken expired = new RefreshToken();
        expired.setToken("expired");
        expired.setUserId(UUID.randomUUID());
        expired.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(refreshTokenRepository.findByToken("expired")).thenReturn(Optional.of(expired));

        assertThrows(InvalidTokenException.class,
                () -> authService.refresh(new RefreshRequest("expired")));
        verify(refreshTokenRepository).delete(expired);
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_success_returnsMessage() {
        doNothing().when(refreshTokenRepository).deleteByToken(any());

        LogoutResponse response = authService.logout(new LogoutRequest("some-token"));

        assertEquals("Logged out successfully", response.getMessage());
        verify(refreshTokenRepository).deleteByToken("some-token");
    }

    @Test
    void logout_nonexistentToken_stillReturnsSuccess() {
        doNothing().when(refreshTokenRepository).deleteByToken(any());

        LogoutResponse response = authService.logout(new LogoutRequest("nonexistent"));

        assertEquals("Logged out successfully", response.getMessage());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User buildUser(String email, String passwordHash, UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setFullName("Test User");
        user.setRole(role);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
