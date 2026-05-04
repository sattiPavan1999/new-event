package com.eventplatform.auth.repository;

import com.eventplatform.auth.entity.User;
import com.eventplatform.auth.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User saveUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("$2a$12$hashed");
        user.setFullName("Test User");
        user.setRole(UserRole.BUYER);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setWalletBalance(new java.math.BigDecimal("10000.00"));
        return userRepository.save(user);
    }

    @Test
    void findByEmail_existingEmail_returnsUser() {
        saveUser("find@example.com");

        Optional<User> result = userRepository.findByEmail("find@example.com");

        assertTrue(result.isPresent());
        assertEquals("find@example.com", result.get().getEmail());
    }

    @Test
    void findByEmail_nonExistingEmail_returnsEmpty() {
        Optional<User> result = userRepository.findByEmail("missing@example.com");

        assertFalse(result.isPresent());
    }

    @Test
    void existsByEmail_existingEmail_returnsTrue() {
        saveUser("exists@example.com");

        assertTrue(userRepository.existsByEmail("exists@example.com"));
    }

    @Test
    void existsByEmail_nonExistingEmail_returnsFalse() {
        assertFalse(userRepository.existsByEmail("nope@example.com"));
    }

    @Test
    void save_persistsAllFields() {
        User user = new User();
        user.setEmail("save@example.com");
        user.setPasswordHash("$2a$12$hash");
        user.setFullName("Full Name");
        user.setRole(UserRole.ORGANISER);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setWalletBalance(new java.math.BigDecimal("10000.00"));
        User saved = userRepository.save(user);

        Optional<User> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("ORGANISER", found.get().getRole().name());
        assertEquals("Full Name", found.get().getFullName());
    }

    @Test
    void findByEmail_caseNotInsensitive() {
        saveUser("lower@example.com");

        assertFalse(userRepository.findByEmail("LOWER@EXAMPLE.COM").isPresent());
    }
}
