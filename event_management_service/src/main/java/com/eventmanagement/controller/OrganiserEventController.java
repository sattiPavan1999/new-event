package com.eventmanagement.controller;

import com.eventmanagement.dto.CreateEventRequest;
import com.eventmanagement.dto.CreateTierRequest;
import com.eventmanagement.dto.EventDetailResponse;
import com.eventmanagement.dto.EventResponse;
import com.eventmanagement.dto.PageResponse;
import com.eventmanagement.dto.SalesSummaryResponse;
import com.eventmanagement.dto.TierResponse;
import com.eventmanagement.service.EventService;
import com.eventmanagement.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/organiser/events")
public class OrganiserEventController {

    private final EventService eventService;
    private final JwtUtil jwtUtil;

    public OrganiserEventController(EventService eventService, JwtUtil jwtUtil) {
        this.eventService = eventService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<PageResponse<EventDetailResponse>> getOrganizerEvents(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID organiserId = jwtUtil.extractOrganiserId(authHeader);
        PageResponse<EventDetailResponse> response = eventService.getOrganizerEvents(organiserId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDetailResponse> getOrganiserEvent(
            @PathVariable("id") UUID eventId,
            @RequestHeader("Authorization") String authHeader) {
        UUID organiserId = jwtUtil.extractOrganiserId(authHeader);
        EventDetailResponse response = eventService.getOrganiserEventDetail(eventId, organiserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID organiserId = jwtUtil.extractOrganiserId(authHeader);
        EventResponse response = eventService.createEvent(request, organiserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/tiers")
    public ResponseEntity<TierResponse> addTier(
            @PathVariable("id") UUID eventId,
            @Valid @RequestBody CreateTierRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID organiserId = jwtUtil.extractOrganiserId(authHeader);
        TierResponse response = eventService.addTier(eventId, request, organiserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/tiers/{tierId}")
    public ResponseEntity<TierResponse> updateTier(
            @PathVariable("id") UUID eventId,
            @PathVariable("tierId") UUID tierId,
            @Valid @RequestBody CreateTierRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID organiserId = jwtUtil.extractOrganiserId(authHeader);
        TierResponse response = eventService.updateTier(eventId, tierId, request, organiserId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/tiers/{tierId}")
    public ResponseEntity<Void> deleteTier(
            @PathVariable("id") UUID eventId,
            @PathVariable("tierId") UUID tierId,
            @RequestHeader("Authorization") String authHeader) {
        UUID organiserId = jwtUtil.extractOrganiserId(authHeader);
        eventService.deleteTier(eventId, tierId, organiserId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable("id") UUID eventId,
            @Valid @RequestBody CreateEventRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID organiserId = jwtUtil.extractOrganiserId(authHeader);
        EventResponse response = eventService.updateEvent(eventId, request, organiserId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<EventResponse> publishEvent(
            @PathVariable("id") UUID eventId,
            @RequestHeader("Authorization") String authHeader) {
        UUID organiserId = jwtUtil.extractOrganiserId(authHeader);
        EventResponse response = eventService.publishEvent(eventId, organiserId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<EventResponse> cancelEvent(
            @PathVariable("id") UUID eventId,
            @RequestHeader("Authorization") String authHeader) {
        UUID organiserId = jwtUtil.extractOrganiserId(authHeader);
        EventResponse response = eventService.cancelEvent(eventId, organiserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<SalesSummaryResponse> getSalesSummary(
            @PathVariable("id") UUID eventId,
            @RequestHeader("Authorization") String authHeader) {
        UUID organiserId = jwtUtil.extractOrganiserId(authHeader);
        SalesSummaryResponse response = eventService.getSalesSummary(eventId, organiserId);
        return ResponseEntity.ok(response);
    }
}
