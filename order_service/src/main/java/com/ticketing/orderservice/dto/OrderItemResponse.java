package com.ticketing.orderservice.dto;

import java.math.BigDecimal;

public class OrderItemResponse {

    private Long tierId;
    private Integer quantity;
    private BigDecimal price;

    public OrderItemResponse() {
    }

    public OrderItemResponse(Long tierId, Integer quantity, BigDecimal price) {
        this.tierId = tierId;
        this.quantity = quantity;
        this.price = price;
    }

    public Long getTierId() {
        return tierId;
    }

    public void setTierId(Long tierId) {
        this.tierId = tierId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
