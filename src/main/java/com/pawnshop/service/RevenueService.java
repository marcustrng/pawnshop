package com.pawnshop.service;

import com.pawnshop.dao.RevenueDAO;
import com.pawnshop.dto.MonthlyRevenueDTO;
import com.pawnshop.dto.RevenueSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class RevenueService {
    private static final Logger logger = LoggerFactory.getLogger(RevenueService.class);
    private final RevenueDAO revenueDAO;

    public RevenueService() {
        this.revenueDAO = new RevenueDAO();
    }

    public RevenueService(RevenueDAO revenueDAO) {
        this.revenueDAO = revenueDAO;
    }

    public List<MonthlyRevenueDTO> getMonthlyRevenue(int months) throws ServiceException {
        try {
            List<MonthlyRevenueDTO> revenueList = revenueDAO.getMonthlyRevenue(months);
            calculateGrowthPercentages(revenueList);
            return revenueList;
        } catch (SQLException e) {
            logger.error("Error fetching monthly revenue", e);
            throw new ServiceException("Failed to fetch monthly revenue", e);
        }
    }

    public List<MonthlyRevenueDTO> getRevenueByYear(int year) throws ServiceException {
        try {
            if (year < 2000 || year > LocalDate.now().getYear()) {
                throw new ServiceException("Invalid year");
            }

            List<MonthlyRevenueDTO> revenueList = revenueDAO.getRevenueByYear(year);
            calculateGrowthPercentages(revenueList);
            return revenueList;
        } catch (SQLException e) {
            logger.error("Error fetching revenue by year", e);
            throw new ServiceException("Failed to fetch revenue by year", e);
        }
    }

    public List<MonthlyRevenueDTO> getRevenueByDateRange(String startDateStr, String endDateStr)
            throws ServiceException {
        try {
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);

            if (startDate.isAfter(endDate)) {
                throw new ServiceException("Start date must be before end date");
            }

            List<MonthlyRevenueDTO> revenueList = revenueDAO.getRevenueByDateRange(
                    Date.valueOf(startDate),
                    Date.valueOf(endDate)
            );
            calculateGrowthPercentages(revenueList);
            return revenueList;
        } catch (SQLException e) {
            logger.error("Error fetching revenue by date range", e);
            throw new ServiceException("Failed to fetch revenue by date range", e);
        }
    }

    public RevenueSummaryDTO getRevenueSummary(int months) throws ServiceException {
        try {
            List<MonthlyRevenueDTO> revenueList = revenueDAO.getMonthlyRevenue(months);
            return calculateSummary(revenueList);
        } catch (SQLException e) {
            logger.error("Error fetching revenue summary", e);
            throw new ServiceException("Failed to fetch revenue summary", e);
        }
    }

    public RevenueSummaryDTO getRevenueSummaryByYear(int year) throws ServiceException {
        try {
            List<MonthlyRevenueDTO> revenueList = revenueDAO.getRevenueByYear(year);
            return calculateSummary(revenueList);
        } catch (SQLException e) {
            logger.error("Error fetching revenue summary by year", e);
            throw new ServiceException("Failed to fetch revenue summary by year", e);
        }
    }

    private void calculateGrowthPercentages(List<MonthlyRevenueDTO> revenueList) {
        for (int i = 0; i < revenueList.size(); i++) {
            if (i < revenueList.size() - 1) {
                MonthlyRevenueDTO current = revenueList.get(i);
                MonthlyRevenueDTO previous = revenueList.get(i + 1);

                BigDecimal currentRevenue = current.getTotalRevenue();
                BigDecimal previousRevenue = previous.getTotalRevenue();

                if (previousRevenue != null && previousRevenue.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal growth = currentRevenue
                            .subtract(previousRevenue)
                            .divide(previousRevenue, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));

                    current.setGrowthPercentage(growth.setScale(2, RoundingMode.HALF_UP));
                } else {
                    current.setGrowthPercentage(BigDecimal.ZERO);
                }
            }
        }
    }

    private RevenueSummaryDTO calculateSummary(List<MonthlyRevenueDTO> revenueList) {
        RevenueSummaryDTO summary = new RevenueSummaryDTO();

        if (revenueList.isEmpty()) {
            summary.setTotalRevenue(BigDecimal.ZERO);
            summary.setTotalLiquidations(0L);
            summary.setAveragePerLiquidation(BigDecimal.ZERO);
            return summary;
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalLiquidations = 0;

        MonthlyRevenueDTO highest = revenueList.stream()
                .max(Comparator.comparing(MonthlyRevenueDTO::getTotalRevenue))
                .orElse(null);

        MonthlyRevenueDTO lowest = revenueList.stream()
                .min(Comparator.comparing(MonthlyRevenueDTO::getTotalRevenue))
                .orElse(null);

        for (MonthlyRevenueDTO revenue : revenueList) {
            totalRevenue = totalRevenue.add(revenue.getTotalRevenue());
            totalLiquidations += revenue.getTotalLiquidations();
        }

        summary.setTotalRevenue(totalRevenue);
        summary.setTotalLiquidations(totalLiquidations);

        if (totalLiquidations > 0) {
            summary.setAveragePerLiquidation(
                    totalRevenue.divide(new BigDecimal(totalLiquidations), 2, RoundingMode.HALF_UP)
            );
        } else {
            summary.setAveragePerLiquidation(BigDecimal.ZERO);
        }

        if (highest != null) {
            summary.setHighestRevenueMonth(highest.getMonth());
            summary.setHighestRevenueAmount(highest.getTotalRevenue());
        }

        if (lowest != null) {
            summary.setLowestRevenueMonth(lowest.getMonth());
            summary.setLowestRevenueAmount(lowest.getTotalRevenue());
        }

        return summary;
    }

    public static class ServiceException extends Exception {
        public ServiceException(String message) {
            super(message);
        }

        public ServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}