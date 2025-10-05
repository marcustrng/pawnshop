package com.pawnshop.dto;

import java.math.BigDecimal;

public class MonthlyRevenueDTO {
    private String month; // Format: yyyy-MM
    private Integer year;
    private Integer monthNumber;
    private Long totalLiquidations;
    private BigDecimal totalRevenue;
    private BigDecimal averageRevenue;
    private BigDecimal growthPercentage;

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonthNumber() {
        return monthNumber;
    }

    public void setMonthNumber(Integer monthNumber) {
        this.monthNumber = monthNumber;
    }

    public Long getTotalLiquidations() {
        return totalLiquidations;
    }

    public void setTotalLiquidations(Long totalLiquidations) {
        this.totalLiquidations = totalLiquidations;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getAverageRevenue() {
        return averageRevenue;
    }

    public void setAverageRevenue(BigDecimal averageRevenue) {
        this.averageRevenue = averageRevenue;
    }

    public BigDecimal getGrowthPercentage() {
        return growthPercentage;
    }

    public void setGrowthPercentage(BigDecimal growthPercentage) {
        this.growthPercentage = growthPercentage;
    }
}
