package com.eventplatform.auth.service;

import com.eventplatform.auth.dto.*;
import com.eventplatform.auth.entity.User;
import com.eventplatform.auth.enums.UserRole;
import com.eventplatform.auth.exception.*;
import com.eventplatform.auth.repository.UserRepository;
import com.eventplatform.auth.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuditService auditService;

    private AuthService authService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, jwtUtil, auditService, encoder);
    }

    // ── Register ──────────────────────────────────────────────────────────────

    @Test
    void register_success_returnTokenAndUser() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(jwtUtil.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        AuthResponse response = authService.register(
                new RegisterRequest("new@example.com", "New User", "Password123", "BUYER"));

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
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

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void login_success_returnsToken() {
        User user = buildUser("login@example.com", encoder.encode("Password123"), UserRole.BUYER);
        when(userRepository.findByEmail("login@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(any(), any(), any())).thenReturn("access");

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

    // ── Logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_returnsSuccessMessage() {
        LogoutResponse response = authService.logout();

        assertEquals("Logged out successfully", response.getMessage());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User buildUser(String email, String passwordHash, UserRole role) {
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setFullName("Test User");
        user.setRole(role);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
