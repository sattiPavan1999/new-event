package com.eventplatform.auth.service;

import com.eventplatform.auth.dto.*;
import com.eventplatform.auth.entity.User;
import com.eventplatform.auth.enums.UserRole;
import com.eventplatform.auth.exception.DuplicateEmailException;
import com.eventplatform.auth.exception.InvalidCredentialsException;
import com.eventplatform.auth.exception.InvalidRoleException;
import com.eventplatform.auth.repository.UserRepository;
import com.eventplatform.auth.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       JwtUtil jwtUtil,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
        this.passwordEncoder = new BCryptPasswordEncoder(12);
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
        user.setWalletBalance(new java.math.BigDecimal("10000.00"));

        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(userId, user.getEmail(), user.getRole().name());

        auditService.logRegistration(request.getEmail(), request.getRole(), true);

        return buildAuthResponse(accessToken, user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            // Constant-time dummy check to prevent email enumeration via timing
            passwordEncoder.matches(request.getPassword(), "$2a$12$dummyHashForTimingProtection.Only.NotARealHash.XXXXXXXXX");
            auditService.logLogin(request.getEmail(), false);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditService.logLogin(request.getEmail(), false);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());

        auditService.logLogin(request.getEmail(), true);

        return buildAuthResponse(accessToken, user);
    }

    public LogoutResponse logout() {
        auditService.logLogout("client", true);
        return new LogoutResponse("Logged out successfully");
    }

    private AuthResponse buildAuthResponse(String accessToken, User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setFullName(user.getFullName());
        userDto.setRole(user.getRole().name());
        userDto.setIsActive(user.getIsActive());
        userDto.setCreatedAt(user.getCreatedAt());
        userDto.setWalletBalance(user.getWalletBalance());

        return new AuthResponse(accessToken, userDto);
    }
}
