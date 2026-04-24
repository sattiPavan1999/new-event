package com.eventmanagement.dto;

import com.eventmanagement.enums.EventCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class EventSummaryResponse {

    private UUID id;
    private String title;
    private EventCategory category;
    private LocalDateTime eventDate;
    private String city;
    private BigDecimal lowestPrice;
    private String bannerImageUrl;
    private String venueName;

    public EventSummaryResponse() {
    }

    public EventSummaryResponse(UUID id, String title, EventCategory category, LocalDateTime eventDate,
                                String city, BigDecimal lowestPrice, String bannerImageUrl, String venueName) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.eventDate = eventDate;
        this.city = city;
        this.lowestPrice = lowestPrice;
        this.bannerImageUrl = bannerImageUrl;
        this.venueName = venueName;
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

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public BigDecimal getLowestPrice() {
        return lowestPrice;
    }

    public void setLowestPrice(BigDecimal lowestPrice) {
        this.lowestPrice = lowestPrice;
    }

    public String getBannerImageUrl() {
        return bannerImageUrl;
    }

    public void setBannerImageUrl(String bannerImageUrl) {
        this.bannerImageUrl = bannerImageUrl;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }
}
