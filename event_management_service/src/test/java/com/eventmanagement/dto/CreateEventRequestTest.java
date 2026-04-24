package com.eventmanagement.dto;

import com.eventmanagement.enums.EventCategory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateEventRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidCreateEventRequest() {
        CreateEventRequest request = new CreateEventRequest(
                "Summer Music Festival",
                "Annual music event",
                EventCategory.CONCERT,
                LocalDateTime.now().plusMonths(1),
                UUID.randomUUID(),
                "https://example.com/banner.jpg"
        );

        Set<ConstraintViolation<CreateEventRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testMissingTitle() {
        CreateEventRequest request = new CreateEventRequest(
                null,
                "Description",
                EventCategory.CONCERT,
                LocalDateTime.now().plusMonths(1),
                UUID.randomUUID(),
                null
        );

        Set<ConstraintViolation<CreateEventRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Title is required")));
    }

    @Test
    void testMissingCategory() {
        CreateEventRequest request = new CreateEventRequest(
                "Summer Music Festival",
                "Description",
                null,
                LocalDateTime.now().plusMonths(1),
                UUID.randomUUID(),
                null
        );

        Set<ConstraintViolation<CreateEventRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Category is required")));
    }

    @Test
    void testMissingEventDate() {
        CreateEventRequest request = new CreateEventRequest(
                "Summer Music Festival",
                "Description",
                EventCategory.CONCERT,
                null,
                UUID.randomUUID(),
                null
        );

        Set<ConstraintViolation<CreateEventRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Event date is required")));
    }

    @Test
    void testMissingVenueId() {
        CreateEventRequest request = new CreateEventRequest(
                "Summer Music Festival",
                "Description",
                EventCategory.CONCERT,
                LocalDateTime.now().plusMonths(1),
                null,
                null
        );

        Set<ConstraintViolation<CreateEventRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Venue ID is required")));
    }

    @Test
    void testTitleExceedsMaxLength() {
        String longTitle = "A".repeat(256);
        CreateEventRequest request = new CreateEventRequest(
                longTitle,
                "Description",
                EventCategory.CONCERT,
                LocalDateTime.now().plusMonths(1),
                UUID.randomUUID(),
                null
        );

        Set<ConstraintViolation<CreateEventRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("255 characters")));
    }

    @Test
    void testGettersAndSetters() {
        CreateEventRequest request = new CreateEventRequest();
        String title = "Test Event";
        String description = "Test Description";
        EventCategory category = EventCategory.SPORTS;
        LocalDateTime eventDate = LocalDateTime.now();
        UUID venueId = UUID.randomUUID();
        String bannerUrl = "https://example.com/banner.jpg";

        request.setTitle(title);
        request.setDescription(description);
        request.setCategory(category);
        request.setEventDate(eventDate);
        request.setVenueId(venueId);
        request.setBannerImageUrl(bannerUrl);

        assertEquals(title, request.getTitle());
        assertEquals(description, request.getDescription());
        assertEquals(category, request.getCategory());
        assertEquals(eventDate, request.getEventDate());
        assertEquals(venueId, request.getVenueId());
        assertEquals(bannerUrl, request.getBannerImageUrl());
    }
}
