package com.pawnshop.dao;

import com.pawnshop.config.DatabaseConfig;
import com.pawnshop.dto.MonthlyRevenueDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RevenueDAO {
    private static final Logger logger = LoggerFactory.getLogger(RevenueDAO.class);

    private static final String GET_MONTHLY_REVENUE =
            "SELECT " +
                    "  DATE_FORMAT(liquidation_date, '%Y-%m') AS month, " +
                    "  YEAR(liquidation_date) AS year, " +
                    "  MONTH(liquidation_date) AS month_number, " +
                    "  COUNT(*) AS total_liquidations, " +
                    "  SUM(price) AS total_revenue, " +
                    "  AVG(price) AS average_revenue " +
                    "FROM liquidation_contract " +
                    "WHERE liquidation_date >= DATE_SUB(CURDATE(), INTERVAL ? MONTH) " +
                    "GROUP BY DATE_FORMAT(liquidation_date, '%Y-%m'), YEAR(liquidation_date), MONTH(liquidation_date) " +
                    "ORDER BY month DESC";

    private static final String GET_REVENUE_BY_DATE_RANGE =
            "SELECT " +
                    "  DATE_FORMAT(liquidation_date, '%Y-%m') AS month, " +
                    "  YEAR(liquidation_date) AS year, " +
                    "  MONTH(liquidation_date) AS month_number, " +
                    "  COUNT(*) AS total_liquidations, " +
                    "  SUM(price) AS total_revenue, " +
                    "  AVG(price) AS average_revenue " +
                    "FROM liquidation_contract " +
                    "WHERE liquidation_date BETWEEN ? AND ? " +
                    "GROUP BY DATE_FORMAT(liquidation_date, '%Y-%m'), YEAR(liquidation_date), MONTH(liquidation_date) " +
                    "ORDER BY month DESC";

    private static final String GET_REVENUE_BY_YEAR =
            "SELECT " +
                    "  DATE_FORMAT(liquidation_date, '%Y-%m') AS month, " +
                    "  YEAR(liquidation_date) AS year, " +
                    "  MONTH(liquidation_date) AS month_number, " +
                    "  COUNT(*) AS total_liquidations, " +
                    "  SUM(price) AS total_revenue, " +
                    "  AVG(price) AS average_revenue " +
                    "FROM liquidation_contract " +
                    "WHERE YEAR(liquidation_date) = ? " +
                    "GROUP BY DATE_FORMAT(liquidation_date, '%Y-%m'), YEAR(liquidation_date), MONTH(liquidation_date) " +
                    "ORDER BY month_number";

    private static final String GET_TOTAL_REVENUE =
            "SELECT " +
                    "  COUNT(*) AS total_liquidations, " +
                    "  COALESCE(SUM(price), 0) AS total_revenue, " +
                    "  COALESCE(AVG(price), 0) AS average_revenue " +
                    "FROM liquidation_contract";

    private static final String GET_TOTAL_REVENUE_BY_DATE_RANGE =
            "SELECT " +
                    "  COUNT(*) AS total_liquidations, " +
                    "  COALESCE(SUM(price), 0) AS total_revenue, " +
                    "  COALESCE(AVG(price), 0) AS average_revenue " +
                    "FROM liquidation_contract " +
                    "WHERE liquidation_date BETWEEN ? AND ?";

    public List<MonthlyRevenueDTO> getMonthlyRevenue(int months) throws SQLException {
        List<MonthlyRevenueDTO> revenueList = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_MONTHLY_REVENUE)) {

            stmt.setInt(1, months);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    revenueList.add(mapResultSetToMonthlyRevenue(rs));
                }
            }
        }

        return revenueList;
    }

    public List<MonthlyRevenueDTO> getRevenueByDateRange(Date startDate, Date endDate) throws SQLException {
        List<MonthlyRevenueDTO> revenueList = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_REVENUE_BY_DATE_RANGE)) {

            stmt.setDate(1, startDate);
            stmt.setDate(2, endDate);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    revenueList.add(mapResultSetToMonthlyRevenue(rs));
                }
            }
        }

        return revenueList;
    }

    public List<MonthlyRevenueDTO> getRevenueByYear(int year) throws SQLException {
        List<MonthlyRevenueDTO> revenueList = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_REVENUE_BY_YEAR)) {

            stmt.setInt(1, year);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    revenueList.add(mapResultSetToMonthlyRevenue(rs));
                }
            }
        }

        return revenueList;
    }

    public MonthlyRevenueDTO getTotalRevenue() throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_TOTAL_REVENUE);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                MonthlyRevenueDTO dto = new MonthlyRevenueDTO();
                dto.setTotalLiquidations(rs.getLong("total_liquidations"));
                dto.setTotalRevenue(rs.getBigDecimal("total_revenue"));
                dto.setAverageRevenue(rs.getBigDecimal("average_revenue"));
                return dto;
            }
        }

        return new MonthlyRevenueDTO();
    }

    public MonthlyRevenueDTO getTotalRevenueByDateRange(Date startDate, Date endDate) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_TOTAL_REVENUE_BY_DATE_RANGE)) {

            stmt.setDate(1, startDate);
            stmt.setDate(2, endDate);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    MonthlyRevenueDTO dto = new MonthlyRevenueDTO();
                    dto.setTotalLiquidations(rs.getLong("total_liquidations"));
                    dto.setTotalRevenue(rs.getBigDecimal("total_revenue"));
                    dto.setAverageRevenue(rs.getBigDecimal("average_revenue"));
                    return dto;
                }
            }
        }

        return new MonthlyRevenueDTO();
    }

    private MonthlyRevenueDTO mapResultSetToMonthlyRevenue(ResultSet rs) throws SQLException {
        MonthlyRevenueDTO dto = new MonthlyRevenueDTO();
        dto.setMonth(rs.getString("month"));
        dto.setYear(rs.getInt("year"));
        dto.setMonthNumber(rs.getInt("month_number"));
        dto.setTotalLiquidations(rs.getLong("total_liquidations"));
        dto.setTotalRevenue(rs.getBigDecimal("total_revenue"));
        dto.setAverageRevenue(rs.getBigDecimal("average_revenue"));
        return dto;
    }
}