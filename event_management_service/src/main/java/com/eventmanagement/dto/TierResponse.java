package com.eventmanagement.dto;

import com.eventmanagement.enums.TierStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TierResponse {

    private Long id;
    private Long eventId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer totalQty;
    private Integer remainingQty;
    private Integer maxPerOrder;
    private LocalDateTime saleStartsAt;
    private LocalDateTime saleEndsAt;
    private TierStatus status;
    private LocalDateTime createdAt;

    public TierResponse() {
    }

    public TierResponse(Long id, Long eventId, String name, String description, BigDecimal price,
                        Integer totalQty, Integer remainingQty, Integer maxPerOrder,
                        LocalDateTime saleStartsAt, LocalDateTime saleEndsAt, TierStatus status,
                        LocalDateTime createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.totalQty = totalQty;
        this.remainingQty = remainingQty;
        this.maxPerOrder = maxPerOrder;
        this.saleStartsAt = saleStartsAt;
        this.saleEndsAt = saleEndsAt;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Integer totalQty) {
        this.totalQty = totalQty;
    }

    public Integer getRemainingQty() {
        return remainingQty;
    }

    public void setRemainingQty(Integer remainingQty) {
        this.remainingQty = remainingQty;
    }

    public Integer getMaxPerOrder() {
        return maxPerOrder;
    }

    public void setMaxPerOrder(Integer maxPerOrder) {
        this.maxPerOrder = maxPerOrder;
    }

    public LocalDateTime getSaleStartsAt() {
        return saleStartsAt;
    }

    public void setSaleStartsAt(LocalDateTime saleStartsAt) {
        this.saleStartsAt = saleStartsAt;
    }

    public LocalDateTime getSaleEndsAt() {
        return saleEndsAt;
    }

    public void setSaleEndsAt(LocalDateTime saleEndsAt) {
        this.saleEndsAt = saleEndsAt;
    }

    public TierStatus getStatus() {
        return status;
    }

    public void setStatus(TierStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
