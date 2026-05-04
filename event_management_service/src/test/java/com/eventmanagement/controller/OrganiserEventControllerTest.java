package com.eventmanagement.controller;

import com.eventmanagement.dto.*;
import com.eventmanagement.enums.EventCategory;
import com.eventmanagement.enums.EventStatus;
import com.eventmanagement.exception.BusinessRuleViolationException;
import com.eventmanagement.exception.ResourceNotFoundException;
import com.eventmanagement.service.EventService;
import com.eventmanagement.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrganiserEventController.class)
class OrganiserEventControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private EventService eventService;
    @MockitoBean private JwtUtil jwtUtil;

    private final Long organiserId = 1L;
    private final Long eventId = 2L;

    @BeforeEach
    void stubJwt() {
        when(jwtUtil.extractOrganiserId(any())).thenReturn(organiserId);
    }

    private EventResponse sampleEventResponse() {
        EventResponse r = new EventResponse();
        r.setId(eventId);
        r.setOrganiserId(organiserId);
        r.setVenueId(3L);
        r.setTitle("Test Event");
        r.setCategory(EventCategory.CONCERT);
        r.setStatus(EventStatus.DRAFT);
        r.setEventDate(LocalDateTime.now().plusDays(30));
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    @Test
    void createEvent_returnsCreated() throws Exception {
        when(eventService.createEvent(any(), any())).thenReturn(sampleEventResponse());

        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Test Event");
        request.setDescription("Desc");
        request.setCategory(EventCategory.CONCERT);
        request.setEventDate(LocalDateTime.now().plusDays(30));
        request.setVenueId(3L);

        mockMvc.perform(post("/api/organiser/events")
                        .header("Authorization", "Bearer fake-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Event"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void getOrganizerEvents_returnsOk() throws Exception {
        PageResponse<EventDetailResponse> page = new PageResponse<>(
                Collections.emptyList(), 0, 10, 0L, 0);
        when(eventService.getOrganizerEvents(any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/organiser/events")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getOrganiserEvent_returnsOk() throws Exception {
        EventDetailResponse detail = new EventDetailResponse();
        detail.setId(eventId);
        detail.setTitle("Test Event");
        detail.setStatus(EventStatus.DRAFT);
        detail.setTiers(Collections.emptyList());
        when(eventService.getOrganiserEventDetail(any(), any())).thenReturn(detail);

        mockMvc.perform(get("/api/organiser/events/{id}", eventId)
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Event"));
    }

    @Test
    void publishEvent_returnsOkWithPublishedStatus() throws Exception {
        EventResponse published = sampleEventResponse();
        published.setStatus(EventStatus.PUBLISHED);
        when(eventService.publishEvent(any(), any())).thenReturn(published);

        mockMvc.perform(patch("/api/organiser/events/{id}/publish", eventId)
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void publishEvent_noActiveTiers_returnsBadRequest() throws Exception {
        when(eventService.publishEvent(any(), any()))
                .thenThrow(new BusinessRuleViolationException("No active tiers"));

        mockMvc.perform(patch("/api/organiser/events/{id}/publish", eventId)
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelEvent_returnsOkWithCancelledStatus() throws Exception {
        EventResponse cancelled = sampleEventResponse();
        cancelled.setStatus(EventStatus.CANCELLED);
        when(eventService.cancelEvent(any(), any())).thenReturn(cancelled);

        mockMvc.perform(patch("/api/organiser/events/{id}/cancel", eventId)
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void addTier_returnsCreated() throws Exception {
        TierResponse tier = new TierResponse();
        tier.setId(4L);
        tier.setEventId(eventId);
        tier.setName("VIP");
        tier.setPrice(new BigDecimal("500.00"));
        tier.setTotalQty(100);
        tier.setRemainingQty(100);
        tier.setStatus(com.eventmanagement.enums.TierStatus.ACTIVE);
        when(eventService.addTier(any(), any(), any())).thenReturn(tier);

        CreateTierRequest request = new CreateTierRequest();
        request.setName("VIP");
        request.setPrice(new BigDecimal("500.00"));
        request.setTotalQty(100);

        mockMvc.perform(post("/api/organiser/events/{id}/tiers", eventId)
                        .header("Authorization", "Bearer fake-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("VIP"));
    }

    @Test
    void getOrganiserEvent_notFound_returnsNotFound() throws Exception {
        when(eventService.getOrganiserEventDetail(any(), any()))
                .thenThrow(new ResourceNotFoundException("Event not found"));

        mockMvc.perform(get("/api/organiser/events/{id}", 99L)
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isNotFound());
    }
}
