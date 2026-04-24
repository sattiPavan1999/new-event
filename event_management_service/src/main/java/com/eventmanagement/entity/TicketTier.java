package com.eventmanagement.entity;

import com.eventmanagement.enums.TierStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_tiers", schema = "events")
public class TicketTier {

    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "total_qty", nullable = false)
    private Integer totalQty;

    @Column(name = "remaining_qty", nullable = false)
    private Integer remainingQty;

    @Column(name = "max_per_order", nullable = false)
    private Integer maxPerOrder;

    @Column(name = "sale_starts_at")
    private LocalDateTime saleStartsAt;

    @Column(name = "sale_ends_at")
    private LocalDateTime saleEndsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TierStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TicketTier() {
    }

    public TicketTier(UUID id, Event event, String name, String description, BigDecimal price,
                      Integer totalQty, Integer remainingQty, Integer maxPerOrder,
                      LocalDateTime saleStartsAt, LocalDateTime saleEndsAt, TierStatus status,
                      LocalDateTime createdAt) {
        this.id = id;
        this.event = event;
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
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
