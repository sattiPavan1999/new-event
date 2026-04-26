package com.eventmanagement.controller;

import com.eventmanagement.dto.*;
import com.eventmanagement.enums.EventCategory;
import com.eventmanagement.enums.EventStatus;
import com.eventmanagement.exception.ResourceNotFoundException;
import com.eventmanagement.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicEventController.class)
class PublicEventControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private EventService eventService;

    private final UUID eventId = UUID.randomUUID();

    @Test
    void browseEvents_noFilters_returnsOk() throws Exception {
        PageResponse<EventSummaryResponse> page = new PageResponse<>(
                Collections.emptyList(), 0, 12, 0L, 0);
        when(eventService.browseEvents(any(), any(), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void browseEvents_withCategoryFilter_returnsOk() throws Exception {
        EventSummaryResponse event = new EventSummaryResponse(
                eventId, "Rock Night", EventCategory.CONCERT,
                LocalDateTime.now().plusDays(10), "Mumbai",
                new BigDecimal("500.00"), null, "Stadium");
        PageResponse<EventSummaryResponse> page = new PageResponse<>(List.of(event), 0, 12, 1L, 1);
        when(eventService.browseEvents(eq(EventCategory.CONCERT), any(), any(), anyInt(), anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/events").param("category", "CONCERT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Rock Night"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void browseEvents_withCitySearch_returnsOk() throws Exception {
        PageResponse<EventSummaryResponse> page = new PageResponse<>(
                Collections.emptyList(), 0, 12, 0L, 0);
        when(eventService.browseEvents(any(), eq("Mumbai"), any(), anyInt(), anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/events").param("city", "Mumbai"))
                .andExpect(status().isOk());
    }

    @Test
    void getEventDetail_existingPublishedEvent_returnsOk() throws Exception {
        EventDetailResponse detail = new EventDetailResponse();
        detail.setId(eventId);
        detail.setTitle("Concert Night");
        detail.setStatus(EventStatus.PUBLISHED);
        detail.setTiers(Collections.emptyList());
        when(eventService.getEventDetail(eventId)).thenReturn(detail);

        mockMvc.perform(get("/api/events/{id}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Concert Night"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void getEventDetail_notFound_returnsNotFound() throws Exception {
        UUID nonExistent = UUID.randomUUID();
        when(eventService.getEventDetail(nonExistent))
                .thenThrow(new ResourceNotFoundException("Event not found"));

        mockMvc.perform(get("/api/events/{id}", nonExistent))
                .andExpect(status().isNotFound());
    }

    @Test
    void browseEvents_invalidPageSize_normalizesToDefault() throws Exception {
        PageResponse<EventSummaryResponse> page = new PageResponse<>(
                Collections.emptyList(), 0, 12, 0L, 0);
        when(eventService.browseEvents(any(), any(), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/events").param("size", "-1"))
                .andExpect(status().isOk());
    }
}
