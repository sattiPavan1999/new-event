package com.eventmanagement.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateTierRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidCreateTierRequest() {
        CreateTierRequest request = new CreateTierRequest(
                "VIP",
                "VIP access with backstage pass",
                new BigDecimal("5000.00"),
                100,
                4,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(30)
        );

        Set<ConstraintViolation<CreateTierRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testMissingName() {
        CreateTierRequest request = new CreateTierRequest(
                null,
                "Description",
                new BigDecimal("1000.00"),
                100,
                4,
                null,
                null
        );

        Set<ConstraintViolation<CreateTierRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Tier name is required")));
    }

    @Test
    void testMissingPrice() {
        CreateTierRequest request = new CreateTierRequest(
                "VIP",
                "Description",
                null,
                100,
                4,
                null,
                null
        );

        Set<ConstraintViolation<CreateTierRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Price is required")));
    }

    @Test
    void testNegativePrice() {
        CreateTierRequest request = new CreateTierRequest(
                "VIP",
                "Description",
                new BigDecimal("-100.00"),
                100,
                4,
                null,
                null
        );

        Set<ConstraintViolation<CreateTierRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("non-negative")));
    }

    @Test
    void testMissingTotalQty() {
        CreateTierRequest request = new CreateTierRequest(
                "VIP",
                "Description",
                new BigDecimal("1000.00"),
                null,
                4,
                null,
                null
        );

        Set<ConstraintViolation<CreateTierRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Total quantity is required")));
    }

    @Test
    void testZeroTotalQty() {
        CreateTierRequest request = new CreateTierRequest(
                "VIP",
                "Description",
                new BigDecimal("1000.00"),
                0,
                4,
                null,
                null
        );

        Set<ConstraintViolation<CreateTierRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("at least 1")));
    }

    @Test
    void testZeroMaxPerOrder() {
        CreateTierRequest request = new CreateTierRequest(
                "VIP",
                "Description",
                new BigDecimal("1000.00"),
                100,
                0,
                null,
                null
        );

        Set<ConstraintViolation<CreateTierRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("at least 1")));
    }

    @Test
    void testNameExceedsMaxLength() {
        String longName = "A".repeat(101);
        CreateTierRequest request = new CreateTierRequest(
                longName,
                "Description",
                new BigDecimal("1000.00"),
                100,
                4,
                null,
                null
        );

        Set<ConstraintViolation<CreateTierRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("100 characters")));
    }

    @Test
    void testGettersAndSetters() {
        CreateTierRequest request = new CreateTierRequest();
        String name = "VIP";
        String description = "VIP Tier";
        BigDecimal price = new BigDecimal("5000.00");
        Integer totalQty = 100;
        Integer maxPerOrder = 4;
        LocalDateTime saleStartsAt = LocalDateTime.now();
        LocalDateTime saleEndsAt = LocalDateTime.now().plusDays(30);

        request.setName(name);
        request.setDescription(description);
        request.setPrice(price);
        request.setTotalQty(totalQty);
        request.setMaxPerOrder(maxPerOrder);
        request.setSaleStartsAt(saleStartsAt);
        request.setSaleEndsAt(saleEndsAt);

        assertEquals(name, request.getName());
        assertEquals(description, request.getDescription());
        assertEquals(price, request.getPrice());
        assertEquals(totalQty, request.getTotalQty());
        assertEquals(maxPerOrder, request.getMaxPerOrder());
        assertEquals(saleStartsAt, request.getSaleStartsAt());
        assertEquals(saleEndsAt, request.getSaleEndsAt());
    }
}
