package com.pawnshop.dao;

import com.pawnshop.config.DatabaseConfig;
import com.pawnshop.model.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerDAO {
    private static final Logger logger = LoggerFactory.getLogger(CustomerDAO.class);

    private static final String INSERT_CUSTOMER =
            "INSERT INTO customer (account_id, full_name, citizen_number, phone_number, " +
                    "address, email, dob) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "SELECT c.*, a.username, a.role, a.is_active FROM customer c " +
                    "JOIN account a ON c.account_id = a.account_id " +
                    "WHERE c.customer_id = ?";

    private static final String SELECT_BY_ACCOUNT_ID =
            "SELECT c.*, a.username, a.role, a.is_active FROM customer c " +
                    "JOIN account a ON c.account_id = a.account_id " +
                    "WHERE c.account_id = ?";

    private static final String SELECT_ALL =
            "SELECT c.*, a.username, a.role, a.is_active FROM customer c " +
                    "JOIN account a ON c.account_id = a.account_id " +
                    "ORDER BY c.created_at DESC";

    private static final String SELECT_ACTIVE =
            "SELECT c.*, a.username, a.role, a.is_active FROM customer c " +
                    "JOIN account a ON c.account_id = a.account_id " +
                    "WHERE a.is_active = TRUE " +
                    "ORDER BY c.full_name";

    private static final String UPDATE_CUSTOMER =
            "UPDATE customer SET full_name = ?, citizen_number = ?, phone_number = ?, " +
                    "address = ?, email = ?, dob = ? WHERE customer_id = ?";

    private static final String DELETE_CUSTOMER =
            "DELETE FROM customer WHERE customer_id = ?";

    private static final String SEARCH_CUSTOMERS =
            "SELECT c.*, a.username, a.role, a.is_active FROM customer c " +
                    "JOIN account a ON c.account_id = a.account_id " +
                    "WHERE (c.full_name LIKE ? OR c.phone_number LIKE ? OR c.email LIKE ? " +
                    "OR c.citizen_number LIKE ? OR c.address LIKE ?) " +
                    "ORDER BY c.full_name";

    private static final String CHECK_CITIZEN_EXISTS =
            "SELECT COUNT(*) FROM customer WHERE citizen_number = ? AND customer_id != ?";

    private static final String CHECK_PHONE_EXISTS =
            "SELECT COUNT(*) FROM customer WHERE phone_number = ? AND customer_id != ?";

    private static final String COUNT_CONTRACTS =
            "SELECT COUNT(*) FROM pawn_contract WHERE customer_id = ?";

    public Integer create(Customer customer) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_CUSTOMER, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, customer.getAccountId());
            stmt.setString(2, customer.getFullName());
            stmt.setString(3, customer.getCitizenNumber());
            stmt.setString(4, customer.getPhoneNumber());
            stmt.setString(5, customer.getAddress());
            stmt.setString(6, customer.getEmail());
            stmt.setDate(7, customer.getDob() != null ? Date.valueOf(customer.getDob()) : null);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating customer failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Integer customerId = generatedKeys.getInt(1);
                    customer.setCustomerId(customerId);
                    logger.info("Customer created successfully with ID: {}", customerId);
                    return customerId;
                } else {
                    throw new SQLException("Creating customer failed, no ID obtained.");
                }
            }
        }
    }

    public Optional<Customer> findById(Integer customerId) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID)) {

            stmt.setInt(1, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCustomer(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Customer> findByAccountId(Integer accountId) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ACCOUNT_ID)) {

            stmt.setInt(1, accountId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCustomer(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Customer> findAll() throws SQLException {
        List<Customer> customers = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                customers.add(mapResultSetToCustomer(rs));
            }
        }

        return customers;
    }

    public List<Customer> findActive() throws SQLException {
        List<Customer> customers = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ACTIVE);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                customers.add(mapResultSetToCustomer(rs));
            }
        }

        return customers;
    }

    public List<Customer> search(String keyword) throws SQLException {
        List<Customer> customers = new ArrayList<>();
        String searchPattern = "%" + keyword + "%";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_CUSTOMERS)) {

            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            stmt.setString(4, searchPattern);
            stmt.setString(5, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapResultSetToCustomer(rs));
                }
            }
        }

        return customers;
    }

    public boolean update(Customer customer) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_CUSTOMER)) {

            stmt.setString(1, customer.getFullName());
            stmt.setString(2, customer.getCitizenNumber());
            stmt.setString(3, customer.getPhoneNumber());
            stmt.setString(4, customer.getAddress());
            stmt.setString(5, customer.getEmail());
            stmt.setDate(6, customer.getDob() != null ? Date.valueOf(customer.getDob()) : null);
            stmt.setInt(7, customer.getCustomerId());

            int affectedRows = stmt.executeUpdate();
            logger.info("Customer updated: ID={}, affected rows={}", customer.getCustomerId(), affectedRows);
            return affectedRows > 0;
        }
    }

    public boolean delete(Integer customerId) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_CUSTOMER)) {

            stmt.setInt(1, customerId);
            int affectedRows = stmt.executeUpdate();
            logger.info("Customer deleted: ID={}, affected rows={}", customerId, affectedRows);
            return affectedRows > 0;
        }
    }

    public boolean isCitizenNumberExists(String citizenNumber, Integer excludeCustomerId) throws SQLException {
        if (citizenNumber == null || citizenNumber.trim().isEmpty()) {
            return false;
        }

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(CHECK_CITIZEN_EXISTS)) {

            stmt.setString(1, citizenNumber);
            stmt.setInt(2, excludeCustomerId != null ? excludeCustomerId : -1);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public boolean isPhoneNumberExists(String phoneNumber, Integer excludeCustomerId) throws SQLException {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(CHECK_PHONE_EXISTS)) {

            stmt.setString(1, phoneNumber);
            stmt.setInt(2, excludeCustomerId != null ? excludeCustomerId : -1);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public int countContracts(Integer customerId) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(COUNT_CONTRACTS)) {

            stmt.setInt(1, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private Customer mapResultSetToCustomer(ResultSet rs) throws SQLException {
        Customer customer = new Customer();
        customer.setCustomerId(rs.getInt("customer_id"));
        customer.setAccountId(rs.getInt("account_id"));
        customer.setFullName(rs.getString("full_name"));
        customer.setCitizenNumber(rs.getString("citizen_number"));
        customer.setPhoneNumber(rs.getString("phone_number"));
        customer.setAddress(rs.getString("address"));
        customer.setEmail(rs.getString("email"));

        Date dob = rs.getDate("dob");
        if (dob != null) {
            customer.setDob(dob.toLocalDate());
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            customer.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            customer.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        // Set joined account info
        customer.setUsername(rs.getString("username"));
        customer.setRole(rs.getString("role"));
        customer.setActive(rs.getBoolean("is_active"));

        return customer;
    }
}