package com.eventmanagement.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SalesSummaryResponse {

    private UUID eventId;
    private String eventTitle;
    private Integer totalOrders;
    private BigDecimal totalRevenue;
    private List<TierSalesDto> tiers = new ArrayList<>();

    public SalesSummaryResponse() {
    }

    public SalesSummaryResponse(UUID eventId, String eventTitle, Integer totalOrders,
                                BigDecimal totalRevenue, List<TierSalesDto> tiers) {
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue;
        this.tiers = tiers;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public List<TierSalesDto> getTiers() {
        return tiers;
    }

    public void setTiers(List<TierSalesDto> tiers) {
        this.tiers = tiers;
    }

    public static class TierSalesDto {
        private UUID tierId;
        private String tierName;
        private Integer totalQty;
        private Integer remainingQty;
        private Integer soldQty;
        private BigDecimal revenue;

        public TierSalesDto() {
        }

        public TierSalesDto(UUID tierId, String tierName, Integer totalQty, Integer remainingQty,
                            Integer soldQty, BigDecimal revenue) {
            this.tierId = tierId;
            this.tierName = tierName;
            this.totalQty = totalQty;
            this.remainingQty = remainingQty;
            this.soldQty = soldQty;
            this.revenue = revenue;
        }

        public UUID getTierId() {
            return tierId;
        }

        public void setTierId(UUID tierId) {
            this.tierId = tierId;
        }

        public String getTierName() {
            return tierName;
        }

        public void setTierName(String tierName) {
            this.tierName = tierName;
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

        public Integer getSoldQty() {
            return soldQty;
        }

        public void setSoldQty(Integer soldQty) {
            this.soldQty = soldQty;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public void setRevenue(BigDecimal revenue) {
            this.revenue = revenue;
        }
    }
}
