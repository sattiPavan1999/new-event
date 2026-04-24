package com.eventmanagement.dto;

import com.eventmanagement.enums.EventCategory;
import com.eventmanagement.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class EventResponse {

    private UUID id;
    private UUID organiserId;
    private UUID venueId;
    private String title;
    private String description;
    private EventCategory category;
    private LocalDateTime eventDate;
    private String bannerImageUrl;
    private EventStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EventResponse() {
    }

    public EventResponse(UUID id, UUID organiserId, UUID venueId, String title, String description,
                         EventCategory category, LocalDateTime eventDate, String bannerImageUrl,
                         EventStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.organiserId = organiserId;
        this.venueId = venueId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.eventDate = eventDate;
        this.bannerImageUrl = bannerImageUrl;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrganiserId() {
        return organiserId;
    }

    public void setOrganiserId(UUID organiserId) {
        this.organiserId = organiserId;
    }

    public UUID getVenueId() {
        return venueId;
    }

    public void setVenueId(UUID venueId) {
        this.venueId = venueId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EventCategory getCategory() {
        return category;
    }

    public void setCategory(EventCategory category) {
        this.category = category;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public String getBannerImageUrl() {
        return bannerImageUrl;
    }

    public void setBannerImageUrl(String bannerImageUrl) {
        this.bannerImageUrl = bannerImageUrl;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
