package com.eventmanagement.service;

import com.eventmanagement.dto.*;
import com.eventmanagement.entity.Event;
import com.eventmanagement.entity.TicketTier;
import com.eventmanagement.entity.Venue;
import com.eventmanagement.enums.EventCategory;
import com.eventmanagement.enums.EventStatus;
import com.eventmanagement.enums.TierStatus;
import com.eventmanagement.exception.BusinessRuleViolationException;
import com.eventmanagement.exception.ResourceNotFoundException;
import com.eventmanagement.repository.EventRepository;
import com.eventmanagement.repository.TicketTierRepository;
import com.eventmanagement.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private TicketTierRepository ticketTierRepository;
    @Mock private AuditService auditService;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository, venueRepository, ticketTierRepository, auditService);
    }

    // ── createEvent ───────────────────────────────────────────────────────────

    @Test
    void createEvent_venueExists_returnsEventResponse() {
        UUID organiserId = UUID.randomUUID();
        Venue venue = buildVenue();
        when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateEventRequest request = buildCreateEventRequest(venue.getId());
        EventResponse response = eventService.createEvent(request, organiserId);

        assertNotNull(response);
        assertEquals("Test Event", response.getTitle());
        assertEquals(EventStatus.DRAFT, response.getStatus());
        verify(auditService).logEventCreated(any(), eq(organiserId), eq("Test Event"));
    }

    @Test
    void createEvent_venueNotFound_throwsResourceNotFoundException() {
        UUID venueId = UUID.randomUUID();
        when(venueRepository.findById(venueId)).thenReturn(Optional.empty());

        CreateEventRequest request = buildCreateEventRequest(venueId);
        assertThrows(ResourceNotFoundException.class,
                () -> eventService.createEvent(request, UUID.randomUUID()));
    }

    // ── addTier ───────────────────────────────────────────────────────────────

    @Test
    void addTier_success_returnsTierResponse() {
        UUID organiserId = UUID.randomUUID();
        Event event = buildEvent(organiserId);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(ticketTierRepository.countByEventId(event.getId())).thenReturn(0L);
        when(ticketTierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateTierRequest request = buildTierRequest();
        TierResponse response = eventService.addTier(event.getId(), request, organiserId);

        assertNotNull(response);
        assertEquals("VIP", response.getName());
        assertEquals(new BigDecimal("1000.00"), response.getPrice());
    }

    @Test
    void addTier_eventNotFound_throwsResourceNotFoundException() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.addTier(eventId, buildTierRequest(), UUID.randomUUID()));
    }

    @Test
    void addTier_ownershipViolation_throwsBusinessRuleViolation() {
        UUID organiserId = UUID.randomUUID();
        Event event = buildEvent(UUID.randomUUID()); // different owner
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        assertThrows(BusinessRuleViolationException.class,
                () -> eventService.addTier(event.getId(), buildTierRequest(), organiserId));
    }

    @Test
    void addTier_maxTiersExceeded_throwsBusinessRuleViolation() {
        UUID organiserId = UUID.randomUUID();
        Event event = buildEvent(organiserId);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(ticketTierRepository.countByEventId(event.getId())).thenReturn(10L);

        assertThrows(BusinessRuleViolationException.class,
                () -> eventService.addTier(event.getId(), buildTierRequest(), organiserId));
    }

    // ── publishEvent ──────────────────────────────────────────────────────────

    @Test
    void publishEvent_draftWithActiveTier_returnsPublishedEvent() {
        UUID organiserId = UUID.randomUUID();
        Event event = buildEvent(organiserId);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(ticketTierRepository.countByEventIdAndStatus(event.getId(), TierStatus.ACTIVE)).thenReturn(1L);
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventResponse response = eventService.publishEvent(event.getId(), organiserId);

        assertEquals(EventStatus.PUBLISHED, response.getStatus());
        verify(auditService).logEventPublished(event.getId(), organiserId);
    }

    @Test
    void publishEvent_noActiveTiers_throwsBusinessRuleViolation() {
        UUID organiserId = UUID.randomUUID();
        Event event = buildEvent(organiserId);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(ticketTierRepository.countByEventIdAndStatus(event.getId(), TierStatus.ACTIVE)).thenReturn(0L);

        assertThrows(BusinessRuleViolationException.class,
                () -> eventService.publishEvent(event.getId(), organiserId));
    }

    @Test
    void publishEvent_alreadyPublished_throwsBusinessRuleViolation() {
        UUID organiserId = UUID.randomUUID();
        Event event = buildEvent(organiserId);
        event.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        assertThrows(BusinessRuleViolationException.class,
                () -> eventService.publishEvent(event.getId(), organiserId));
    }

    // ── cancelEvent ───────────────────────────────────────────────────────────

    @Test
    void cancelEvent_success_returnsCancelledEvent() {
        UUID organiserId = UUID.randomUUID();
        Event event = buildEvent(organiserId);
        event.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventResponse response = eventService.cancelEvent(event.getId(), organiserId);

        assertEquals(EventStatus.CANCELLED, response.getStatus());
    }

    // ── getEventDetail ────────────────────────────────────────────────────────

    @Test
    void getEventDetail_publishedEvent_returnsDetail() {
        Event event = buildEvent(UUID.randomUUID());
        event.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        EventDetailResponse response = eventService.getEventDetail(event.getId());

        assertNotNull(response);
        assertEquals("Test Event", response.getTitle());
    }

    @Test
    void getEventDetail_draftEvent_throwsResourceNotFoundException() {
        Event event = buildEvent(UUID.randomUUID());
        // status is DRAFT by default in buildEvent
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.getEventDetail(event.getId()));
    }

    @Test
    void getEventDetail_notFound_throwsResourceNotFoundException() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.getEventDetail(eventId));
    }

    // ── browseEvents ──────────────────────────────────────────────────────────

    @Test
    void browseEvents_returnsPagedResults() {
        Event event = buildEvent(UUID.randomUUID());
        event.setStatus(EventStatus.PUBLISHED);
        Page<Event> page = new PageImpl<>(List.of(event));
        when(eventRepository.findPublishedEvents(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<EventSummaryResponse> response = eventService.browseEvents(null, null, null, 0, 12);

        assertEquals(1, response.getContent().size());
        assertEquals(1L, response.getTotalElements());
    }

    @Test
    void browseEvents_emptyResult_returnsEmptyPage() {
        Page<Event> emptyPage = new PageImpl<>(Collections.emptyList());
        when(eventRepository.findPublishedEvents(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        PageResponse<EventSummaryResponse> response = eventService.browseEvents(null, null, null, 0, 12);

        assertEquals(0, response.getContent().size());
    }

    // ── getSalesSummary ───────────────────────────────────────────────────────

    @Test
    void getSalesSummary_success_returnsCorrectTotals() {
        UUID organiserId = UUID.randomUUID();
        Event event = buildEvent(organiserId);
        TicketTier tier = buildTier(event, 100, 80); // 20 sold
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(ticketTierRepository.findByEventId(event.getId())).thenReturn(List.of(tier));

        SalesSummaryResponse response = eventService.getSalesSummary(event.getId(), organiserId);

        assertEquals(20, response.getTotalOrders());
        assertEquals(0, new BigDecimal("20000.00").compareTo(response.getTotalRevenue()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Venue buildVenue() {
        Venue venue = new Venue();
        venue.setId(UUID.randomUUID());
        venue.setName("Test Venue");
        venue.setAddress("123 Main St");
        venue.setCity("Mumbai");
        venue.setCountry("India");
        venue.setCapacity(1000);
        return venue;
    }

    private Event buildEvent(UUID organiserId) {
        Venue venue = buildVenue();
        Event event = new Event(
                UUID.randomUUID(), organiserId, venue,
                "Test Event", "Description", EventCategory.CONCERT,
                LocalDateTime.now().plusDays(30), null,
                EventStatus.DRAFT, LocalDateTime.now(), LocalDateTime.now()
        );
        return event;
    }

    private CreateEventRequest buildCreateEventRequest(UUID venueId) {
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Test Event");
        request.setDescription("Description");
        request.setCategory(EventCategory.CONCERT);
        request.setEventDate(LocalDateTime.now().plusDays(30));
        request.setVenueId(venueId);
        return request;
    }

    private CreateTierRequest buildTierRequest() {
        CreateTierRequest request = new CreateTierRequest();
        request.setName("VIP");
        request.setDescription("VIP tier");
        request.setPrice(new BigDecimal("1000.00"));
        request.setTotalQty(100);
        request.setMaxPerOrder(5);
        return request;
    }

    private TicketTier buildTier(Event event, int totalQty, int remainingQty) {
        TicketTier tier = new TicketTier();
        tier.setId(UUID.randomUUID());
        tier.setEvent(event);
        tier.setName("General");
        tier.setPrice(new BigDecimal("1000.00"));
        tier.setTotalQty(totalQty);
        tier.setRemainingQty(remainingQty);
        tier.setMaxPerOrder(10);
        tier.setStatus(TierStatus.ACTIVE);
        tier.setCreatedAt(LocalDateTime.now());
        return tier;
    }
}
