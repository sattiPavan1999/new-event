package com.eventmanagement.controller;

import com.eventmanagement.dto.EventDetailResponse;
import com.eventmanagement.dto.EventSummaryResponse;
import com.eventmanagement.dto.PageResponse;
import com.eventmanagement.enums.EventCategory;
import com.eventmanagement.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/events")
public class PublicEventController {

    private final EventService eventService;

    public PublicEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<EventSummaryResponse>> browseEvents(
            @RequestParam(value = "category", required = false) EventCategory category,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size) {

        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 100) {
            size = 12;
        }

        PageResponse<EventSummaryResponse> response = eventService.browseEvents(category, city, search, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDetailResponse> getEventDetail(@PathVariable("id") UUID eventId) {
        EventDetailResponse response = eventService.getEventDetail(eventId);
        return ResponseEntity.ok(response);
    }
}
