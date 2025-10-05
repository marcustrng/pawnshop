package com.pawnshop.dto;

import java.math.BigDecimal;

public class RevenueSummaryDTO {
    private BigDecimal totalRevenue;
    private Long totalLiquidations;
    private BigDecimal averagePerLiquidation;
    private String highestRevenueMonth;
    private BigDecimal highestRevenueAmount;
    private String lowestRevenueMonth;
    private BigDecimal lowestRevenueAmount;

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Long getTotalLiquidations() {
        return totalLiquidations;
    }

    public void setTotalLiquidations(Long totalLiquidations) {
        this.totalLiquidations = totalLiquidations;
    }

    public BigDecimal getAveragePerLiquidation() {
        return averagePerLiquidation;
    }

    public void setAveragePerLiquidation(BigDecimal averagePerLiquidation) {
        this.averagePerLiquidation = averagePerLiquidation;
    }

    public String getHighestRevenueMonth() {
        return highestRevenueMonth;
    }

    public void setHighestRevenueMonth(String highestRevenueMonth) {
        this.highestRevenueMonth = highestRevenueMonth;
    }

    public BigDecimal getHighestRevenueAmount() {
        return highestRevenueAmount;
    }

    public void setHighestRevenueAmount(BigDecimal highestRevenueAmount) {
        this.highestRevenueAmount = highestRevenueAmount;
    }

    public String getLowestRevenueMonth() {
        return lowestRevenueMonth;
    }

    public void setLowestRevenueMonth(String lowestRevenueMonth) {
        this.lowestRevenueMonth = lowestRevenueMonth;
    }

    public BigDecimal getLowestRevenueAmount() {
        return lowestRevenueAmount;
    }

    public void setLowestRevenueAmount(BigDecimal lowestRevenueAmount) {
        this.lowestRevenueAmount = lowestRevenueAmount;
    }
}