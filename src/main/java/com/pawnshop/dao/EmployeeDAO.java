package com.pawnshop.dao;

import com.pawnshop.config.DatabaseConfig;
import com.pawnshop.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EmployeeDAO {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeDAO.class);

    private static final String INSERT_EMPLOYEE =
            "INSERT INTO employee (account_id, full_name, dob, phone_number, salary, email, " +
                    "citizen_number, hire_date, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "SELECT e.*, a.username, a.role FROM employee e " +
                    "JOIN account a ON e.account_id = a.account_id " +
                    "WHERE e.employee_id = ?";

    private static final String SELECT_BY_ACCOUNT_ID =
            "SELECT e.*, a.username, a.role FROM employee e " +
                    "JOIN account a ON e.account_id = a.account_id " +
                    "WHERE e.account_id = ?";

    private static final String SELECT_ALL =
            "SELECT e.*, a.username, a.role FROM employee e " +
                    "JOIN account a ON e.account_id = a.account_id " +
                    "ORDER BY e.created_at DESC";

    private static final String SELECT_ACTIVE =
            "SELECT e.*, a.username, a.role FROM employee e " +
                    "JOIN account a ON e.account_id = a.account_id " +
                    "WHERE e.is_active = TRUE " +
                    "ORDER BY e.full_name";

    private static final String UPDATE_EMPLOYEE =
            "UPDATE employee SET full_name = ?, dob = ?, phone_number = ?, salary = ?, " +
                    "email = ?, citizen_number = ?, hire_date = ?, is_active = ? " +
                    "WHERE employee_id = ?";

    private static final String DELETE_EMPLOYEE =
            "DELETE FROM employee WHERE employee_id = ?";

    private static final String CHECK_CITIZEN_EXISTS =
            "SELECT COUNT(*) FROM employee WHERE citizen_number = ? AND employee_id != ?";

    private static final String CHECK_PHONE_EXISTS =
            "SELECT COUNT(*) FROM employee WHERE phone_number = ? AND employee_id != ?";

    private static final String CHECK_EMAIL_EXISTS =
            "SELECT COUNT(*) FROM employee WHERE email = ? AND employee_id != ?";

    private static final String SEARCH_EMPLOYEES =
            "SELECT e.*, a.username, a.role FROM employee e " +
                    "JOIN account a ON e.account_id = a.account_id " +
                    "WHERE (e.full_name LIKE ? OR e.phone_number LIKE ? OR e.email LIKE ? " +
                    "OR e.citizen_number LIKE ?) " +
                    "ORDER BY e.full_name";

    public Integer create(Employee employee) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_EMPLOYEE, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, employee.getAccountId());
            stmt.setString(2, employee.getFullName());
            stmt.setDate(3, employee.getDob() != null ? Date.valueOf(employee.getDob()) : null);
            stmt.setString(4, employee.getPhoneNumber());
            stmt.setBigDecimal(5, employee.getSalary());
            stmt.setString(6, employee.getEmail());
            stmt.setString(7, employee.getCitizenNumber());
            stmt.setDate(8, employee.getHireDate() != null ? Date.valueOf(employee.getHireDate()) : null);
            stmt.setBoolean(9, employee.isActive());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating employee failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Integer employeeId = generatedKeys.getInt(1);
                    employee.setEmployeeId(employeeId);
                    logger.info("Employee created successfully with ID: {}", employeeId);
                    return employeeId;
                } else {
                    throw new SQLException("Creating employee failed, no ID obtained.");
                }
            }
        }
    }

    public Optional<Employee> findById(Integer employeeId) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEmployee(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Employee> findByAccountId(Integer accountId) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ACCOUNT_ID)) {

            stmt.setInt(1, accountId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEmployee(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Employee> findAll() throws SQLException {
        List<Employee> employees = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                employees.add(mapResultSetToEmployee(rs));
            }
        }

        return employees;
    }

    public List<Employee> findActive() throws SQLException {
        List<Employee> employees = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ACTIVE);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                employees.add(mapResultSetToEmployee(rs));
            }
        }

        return employees;
    }

    public List<Employee> search(String keyword) throws SQLException {
        List<Employee> employees = new ArrayList<>();
        String searchPattern = "%" + keyword + "%";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_EMPLOYEES)) {

            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            stmt.setString(4, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    employees.add(mapResultSetToEmployee(rs));
                }
            }
        }

        return employees;
    }

    public boolean update(Employee employee) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_EMPLOYEE)) {

            stmt.setString(1, employee.getFullName());
            stmt.setDate(2, employee.getDob() != null ? Date.valueOf(employee.getDob()) : null);
            stmt.setString(3, employee.getPhoneNumber());
            stmt.setBigDecimal(4, employee.getSalary());
            stmt.setString(5, employee.getEmail());
            stmt.setString(6, employee.getCitizenNumber());
            stmt.setDate(7, employee.getHireDate() != null ? Date.valueOf(employee.getHireDate()) : null);
            stmt.setBoolean(8, employee.isActive());
            stmt.setInt(9, employee.getEmployeeId());

            int affectedRows = stmt.executeUpdate();
            logger.info("Employee updated: ID={}, affected rows={}", employee.getEmployeeId(), affectedRows);
            return affectedRows > 0;
        }
    }

    public boolean delete(Integer employeeId) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_EMPLOYEE)) {

            stmt.setInt(1, employeeId);
            int affectedRows = stmt.executeUpdate();
            logger.info("Employee deleted: ID={}, affected rows={}", employeeId, affectedRows);
            return affectedRows > 0;
        }
    }

    public boolean isCitizenNumberExists(String citizenNumber, Integer excludeEmployeeId) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(CHECK_CITIZEN_EXISTS)) {

            stmt.setString(1, citizenNumber);
            stmt.setInt(2, excludeEmployeeId != null ? excludeEmployeeId : -1);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public boolean isPhoneNumberExists(String phoneNumber, Integer excludeEmployeeId) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(CHECK_PHONE_EXISTS)) {

            stmt.setString(1, phoneNumber);
            stmt.setInt(2, excludeEmployeeId != null ? excludeEmployeeId : -1);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public boolean isEmailExists(String email, Integer excludeEmployeeId) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(CHECK_EMAIL_EXISTS)) {

            stmt.setString(1, email);
            stmt.setInt(2, excludeEmployeeId != null ? excludeEmployeeId : -1);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private Employee mapResultSetToEmployee(ResultSet rs) throws SQLException {
        Employee employee = new Employee();
        employee.setEmployeeId(rs.getInt("employee_id"));
        employee.setAccountId(rs.getInt("account_id"));
        employee.setFullName(rs.getString("full_name"));

        Date dob = rs.getDate("dob");
        if (dob != null) {
            employee.setDob(dob.toLocalDate());
        }

        employee.setPhoneNumber(rs.getString("phone_number"));
        employee.setSalary(rs.getBigDecimal("salary"));
        employee.setEmail(rs.getString("email"));
        employee.setCitizenNumber(rs.getString("citizen_number"));

        Date hireDate = rs.getDate("hire_date");
        if (hireDate != null) {
            employee.setHireDate(hireDate.toLocalDate());
        }

        employee.setActive(rs.getBoolean("is_active"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            employee.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            employee.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        // Set joined account info
        employee.setUsername(rs.getString("username"));
        employee.setRole(rs.getString("role"));

        return employee;
    }
}