package com.eventmanagement.dto;

import com.eventmanagement.enums.EventCategory;
import com.eventmanagement.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EventDetailResponse {

    private UUID id;
    private String title;
    private String description;
    private EventCategory category;
    private LocalDateTime eventDate;
    private String bannerImageUrl;
    private EventStatus status;
    private VenueDto venue;
    private List<TierResponse> tiers = new ArrayList<>();

    public EventDetailResponse() {
    }

    public EventDetailResponse(UUID id, String title, String description, EventCategory category,
                               LocalDateTime eventDate, String bannerImageUrl, EventStatus status,
                               VenueDto venue, List<TierResponse> tiers) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.eventDate = eventDate;
        this.bannerImageUrl = bannerImageUrl;
        this.status = status;
        this.venue = venue;
        this.tiers = tiers;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public VenueDto getVenue() {
        return venue;
    }

    public void setVenue(VenueDto venue) {
        this.venue = venue;
    }

    public List<TierResponse> getTiers() {
        return tiers;
    }

    public void setTiers(List<TierResponse> tiers) {
        this.tiers = tiers;
    }
}
