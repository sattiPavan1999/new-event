package com.eventmanagement.repository;

import com.eventmanagement.entity.Event;
import com.eventmanagement.entity.Venue;
import com.eventmanagement.enums.EventCategory;
import com.eventmanagement.enums.EventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventRepositoryTest {

    @Autowired private EventRepository eventRepository;
    @Autowired private VenueRepository venueRepository;

    private UUID organiserId;
    private Venue venue;

    @BeforeEach
    void setUp() {
        organiserId = UUID.randomUUID();
        venue = new Venue();
        venue.setId(UUID.randomUUID());
        venue.setName("Test Venue");
        venue.setAddress("123 Main St");
        venue.setCity("Mumbai");
        venue.setCountry("India");
        venue.setCapacity(500);
        venueRepository.save(venue);
    }

    private Event saveEvent(UUID orgId, EventStatus status, LocalDateTime eventDate) {
        Event e = new Event(UUID.randomUUID(), orgId, venue,
                "Event " + UUID.randomUUID().toString().substring(0, 8),
                "Desc", EventCategory.CONCERT, eventDate, null, status,
                LocalDateTime.now(), LocalDateTime.now());
        return eventRepository.save(e);
    }

    @Test
    void findByOrganiserIdOrderByCreatedAtDesc_returnsOnlyOrganizerEvents() {
        saveEvent(organiserId, EventStatus.DRAFT, LocalDateTime.now().plusDays(10));
        saveEvent(organiserId, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(20));
        saveEvent(UUID.randomUUID(), EventStatus.DRAFT, LocalDateTime.now().plusDays(5));

        Page<Event> page = eventRepository.findByOrganiserIdOrderByCreatedAtDesc(
                organiserId, PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(e -> e.getOrganiserId().equals(organiserId)));
    }

    @Test
    void findByOrganiserIdOrderByCreatedAtDesc_emptyForUnknownOrganizer() {
        Page<Event> page = eventRepository.findByOrganiserIdOrderByCreatedAtDesc(
                UUID.randomUUID(), PageRequest.of(0, 10));

        assertEquals(0, page.getTotalElements());
    }

    @Test
    void findPublishedEvents_returnsOnlyFuturePublishedEvents() {
        saveEvent(organiserId, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(5));
        saveEvent(organiserId, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(10));
        saveEvent(organiserId, EventStatus.DRAFT, LocalDateTime.now().plusDays(5));

        Page<Event> page = eventRepository.findPublishedEvents(
                EventStatus.PUBLISHED, LocalDateTime.now(), null, null, null,
                PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream()
                .allMatch(e -> e.getStatus() == EventStatus.PUBLISHED));
    }

    @Test
    void findPublishedEvents_excludesPastEvents() {
        saveEvent(organiserId, EventStatus.PUBLISHED, LocalDateTime.now().minusDays(1)); // past
        saveEvent(organiserId, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(5));  // future

        Page<Event> page = eventRepository.findPublishedEvents(
                EventStatus.PUBLISHED, LocalDateTime.now(), null, null, null,
                PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
    }

    @Test
    void findPublishedEvents_filterByCategory() {
        saveEvent(organiserId, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(5));
        Event sportsEvent = new Event(UUID.randomUUID(), organiserId, venue,
                "Sports Day", "Desc", EventCategory.SPORTS,
                LocalDateTime.now().plusDays(5), null, EventStatus.PUBLISHED,
                LocalDateTime.now(), LocalDateTime.now());
        eventRepository.save(sportsEvent);

        Page<Event> page = eventRepository.findPublishedEvents(
                EventStatus.PUBLISHED, LocalDateTime.now(), EventCategory.SPORTS, null, null,
                PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals(EventCategory.SPORTS, page.getContent().get(0).getCategory());
    }

    @Test
    void findAll_returnsAllSavedEvents() {
        saveEvent(organiserId, EventStatus.DRAFT, LocalDateTime.now().plusDays(1));
        saveEvent(organiserId, EventStatus.DRAFT, LocalDateTime.now().plusDays(2));

        List<Event> all = eventRepository.findAll();

        assertTrue(all.size() >= 2);
    }
}
