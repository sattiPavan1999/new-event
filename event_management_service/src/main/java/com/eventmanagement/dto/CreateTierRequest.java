package com.eventmanagement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateTierRequest {

    @NotBlank(message = "Tier name is required")
    @Size(max = 100, message = "Tier name cannot exceed 100 characters")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
    private BigDecimal price;

    @NotNull(message = "Total quantity is required")
    @Min(value = 1, message = "Total quantity must be at least 1")
    private Integer totalQty;

    @Min(value = 1, message = "Max per order must be at least 1")
    private Integer maxPerOrder;

    private LocalDateTime saleStartsAt;

    private LocalDateTime saleEndsAt;

    public CreateTierRequest() {
    }

    public CreateTierRequest(String name, String description, BigDecimal price, Integer totalQty,
                             Integer maxPerOrder, LocalDateTime saleStartsAt, LocalDateTime saleEndsAt) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.totalQty = totalQty;
        this.maxPerOrder = maxPerOrder;
        this.saleStartsAt = saleStartsAt;
        this.saleEndsAt = saleEndsAt;
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
}
