package com.ticketing.orderservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class OrderItemDetail {

    private Long orderItemId;
    private Long tierId;
    private String tierName;
    private String eventTitle;
    private Instant eventDate;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Instant createdAt;

    public OrderItemDetail() {
    }

    public OrderItemDetail(Long orderItemId, Long tierId, String tierName, String eventTitle, Instant eventDate, Integer quantity, BigDecimal unitPrice, Instant createdAt) {
        this.orderItemId = orderItemId;
        this.tierId = tierId;
        this.tierName = tierName;
        this.eventTitle = eventTitle;
        this.eventDate = eventDate;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.createdAt = createdAt;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Long getTierId() {
        return tierId;
    }

    public void setTierId(Long tierId) {
        this.tierId = tierId;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public Instant getEventDate() {
        return eventDate;
    }

    public void setEventDate(Instant eventDate) {
        this.eventDate = eventDate;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
