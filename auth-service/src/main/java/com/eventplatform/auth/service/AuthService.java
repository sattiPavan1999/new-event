package com.eventplatform.auth.service;

import com.eventplatform.auth.dto.*;
import com.eventplatform.auth.entity.RefreshToken;
import com.eventplatform.auth.entity.User;
import com.eventplatform.auth.enums.UserRole;
import com.eventplatform.auth.exception.DuplicateEmailException;
import com.eventplatform.auth.exception.InvalidCredentialsException;
import com.eventplatform.auth.exception.InvalidRoleException;
import com.eventplatform.auth.exception.InvalidTokenException;
import com.eventplatform.auth.repository.RefreshTokenRepository;
import com.eventplatform.auth.repository.UserRepository;
import com.eventplatform.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final jakarta.persistence.EntityManager entityManager;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtUtil jwtUtil,
                       AuditService auditService,
                       jakarta.persistence.EntityManager entityManager) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
        this.passwordEncoder = new BCryptPasswordEncoder(12);
        this.entityManager = entityManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            auditService.logRegistration(request.getEmail(), request.getRole(), false);
            throw new DuplicateEmailException("Email already registered");
        }

        UserRole role;
        try {
            role = UserRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            auditService.logRegistration(request.getEmail(), request.getRole(), false);
            throw new InvalidRoleException("Invalid role. Must be BUYER or ORGANISER");
        }

        if (role == UserRole.ADMIN) {
            auditService.logRegistration(request.getEmail(), request.getRole(), false);
            throw new InvalidRoleException("ADMIN role cannot be self-assigned");
        }

        UUID userId = UUID.randomUUID();
        String passwordHash = passwordEncoder.encode(request.getPassword());
        LocalDateTime now = LocalDateTime.now();

        User user = new User();
        user.setId(userId);
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordHash);
        user.setFullName(request.getFullName());
        user.setRole(role);
        user.setIsActive(true);
        user.setCreatedAt(now);

        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(userId, user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(userId);

        saveRefreshToken(userId, refreshToken);

        auditService.logRegistration(request.getEmail(), request.getRole(), true);

        return buildAuthResponse(accessToken, refreshToken, user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    auditService.logLogin(request.getEmail(), false);
                    return new InvalidCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditService.logLogin(request.getEmail(), false);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        saveRefreshToken(user.getId(), refreshToken);

        auditService.logLogin(request.getEmail(), true);

        return buildAuthResponse(accessToken, refreshToken, user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String token = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    auditService.logTokenRefresh("unknown", false);
                    return new InvalidTokenException("Invalid or expired refresh token");
                });

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            auditService.logTokenRefresh(refreshToken.getUserId().toString(), false);
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        try {
            Claims claims = jwtUtil.validateToken(token);
            UUID userId = UUID.fromString(claims.getSubject());

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new InvalidTokenException("User not found"));

            refreshTokenRepository.delete(refreshToken);
            entityManager.flush();

            String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
            String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

            saveRefreshToken(user.getId(), newRefreshToken);

            auditService.logTokenRefresh(userId.toString(), true);

            return buildAuthResponse(newAccessToken, newRefreshToken, user);
        } catch (Exception e) {
            auditService.logTokenRefresh("unknown", false);
            throw new InvalidTokenException("Invalid or expired refresh token");
        }
    }

    @Transactional
    public LogoutResponse logout(LogoutRequest request) {
        String token = request.getRefreshToken();

        try {
            refreshTokenRepository.deleteByToken(token);
            Claims claims = jwtUtil.validateToken(token);
            UUID userId = UUID.fromString(claims.getSubject());
            auditService.logLogout(userId.toString(), true);
        } catch (Exception e) {
            auditService.logLogout("unknown", true);
        }

        return new LogoutResponse("Logged out successfully");
    }

    private void saveRefreshToken(UUID userId, String token) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(jwtUtil.getRefreshTokenExpiryMillis() / 1000);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUserId(userId);
        refreshToken.setToken(token);
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setCreatedAt(now);

        refreshTokenRepository.save(refreshToken);
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setFullName(user.getFullName());
        userDto.setRole(user.getRole().name());
        userDto.setIsActive(user.getIsActive());
        userDto.setCreatedAt(user.getCreatedAt());

        return new AuthResponse(accessToken, refreshToken, userDto);
    }
}
