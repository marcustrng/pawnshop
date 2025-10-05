package com.pawnshop.service;

import com.pawnshop.dao.AccountDAO;
import com.pawnshop.dao.EmployeeDAO;
import com.pawnshop.dto.EmployeeRequestDTO;
import com.pawnshop.dto.EmployeeResponseDTO;
import com.pawnshop.model.Account;
import com.pawnshop.model.Employee;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EmployeeService {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);
    private final EmployeeDAO employeeDAO;
    private final AccountDAO accountDAO;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public EmployeeService() {
        this.employeeDAO = new EmployeeDAO();
        this.accountDAO = new AccountDAO();
    }

    public EmployeeService(EmployeeDAO employeeDAO, AccountDAO accountDAO) {
        this.employeeDAO = employeeDAO;
        this.accountDAO = accountDAO;
    }

    /**
     * Create new employee with existing account
     */
    public EmployeeResponseDTO createEmployee(EmployeeRequestDTO dto) throws ServiceException {
        try {
            // Validate input
            validateEmployeeRequest(dto, null);

            // Check if account exists
            if (dto.getAccountId() == null) {
                throw new ServiceException("Account ID is required");
            }

            Optional<Account> accountOpt = accountDAO.findById(dto.getAccountId());
            if (accountOpt.isEmpty()) {
                throw new ServiceException("Account not found");
            }

            Account account = accountOpt.get();
            if (account.getRole() != Account.Role.EMPLOYEE && account.getRole() != Account.Role.ADMIN) {
                throw new ServiceException("Account must have employee or admin role");
            }

            // Check if employee already exists for this account
            Optional<Employee> existingEmployee = employeeDAO.findByAccountId(dto.getAccountId());
            if (existingEmployee.isPresent()) {
                throw new ServiceException("Employee profile already exists for this account");
            }

            // Create employee
            Employee employee = mapDtoToEmployee(dto);
            employee.setAccountId(dto.getAccountId());
            employee.setActive(true);

            Integer employeeId = employeeDAO.create(employee);
            employee.setEmployeeId(employeeId);

            logger.info("Employee created successfully: ID={}, account={}",
                    employeeId, account.getUsername());

            return toResponseDTO(employee, account);

        } catch (SQLException e) {
            logger.error("Database error creating employee", e);
            throw new ServiceException("Failed to create employee", e);
        }
    }

    /**
     * Create employee with new account (one transaction)
     */
    public EmployeeResponseDTO createEmployeeWithAccount(EmployeeRequestDTO dto) throws ServiceException {
        try {
            // Validate employee data
            validateEmployeeRequest(dto, null);

            // Validate account data
            if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
                throw new ServiceException("Username is required");
            }
            if (dto.getPassword() == null || dto.getPassword().length() < 6) {
                throw new ServiceException("Password must be at least 6 characters");
            }

            // Check username availability
            if (accountDAO.isUsernameExists(dto.getUsername(), null)) {
                throw new ServiceException("Username already exists");
            }

            // Create account first
            Account account = new Account();
            account.setUsername(dto.getUsername());
            account.setPasswordHash(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(12)));
            account.setRole(Account.Role.EMPLOYEE);
            account.setActive(true);

            Integer accountId = accountDAO.create(account);
            account.setAccountId(accountId);

            // Create employee
            Employee employee = mapDtoToEmployee(dto);
            employee.setAccountId(accountId);
            employee.setActive(true);

            Integer employeeId = employeeDAO.create(employee);
            employee.setEmployeeId(employeeId);

            logger.info("Employee with account created: employeeId={}, username={}",
                    employeeId, dto.getUsername());

            return toResponseDTO(employee, account);

        } catch (SQLException e) {
            logger.error("Database error creating employee with account", e);
            throw new ServiceException("Failed to create employee with account", e);
        }
    }

    /**
     * Get employee by ID
     */
    public EmployeeResponseDTO getEmployeeById(Integer employeeId) throws ServiceException {
        try {
            Optional<Employee> employeeOpt = employeeDAO.findById(employeeId);
            if (employeeOpt.isEmpty()) {
                throw new ServiceException("Employee not found");
            }

            Employee employee = employeeOpt.get();
            Optional<Account> accountOpt = accountDAO.findById(employee.getAccountId());

            return toResponseDTO(employee, accountOpt.orElse(null));

        } catch (SQLException e) {
            logger.error("Error fetching employee: ID={}", employeeId, e);
            throw new ServiceException("Failed to fetch employee", e);
        }
    }

    /**
     * Get all employees
     */
    public List<EmployeeResponseDTO> getAllEmployees() throws ServiceException {
        try {
            List<Employee> employees = employeeDAO.findAll();
            return employees.stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            logger.error("Error fetching all employees", e);
            throw new ServiceException("Failed to fetch employees", e);
        }
    }

    /**
     * Get active employees only
     */
    public List<EmployeeResponseDTO> getActiveEmployees() throws ServiceException {
        try {
            List<Employee> employees = employeeDAO.findActive();
            return employees.stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            logger.error("Error fetching active employees", e);
            throw new ServiceException("Failed to fetch active employees", e);
        }
    }

    /**
     * Search employees
     */
    public List<EmployeeResponseDTO> searchEmployees(String keyword) throws ServiceException {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return getAllEmployees();
            }

            List<Employee> employees = employeeDAO.search(keyword.trim());
            return employees.stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            logger.error("Error searching employees", e);
            throw new ServiceException("Failed to search employees", e);
        }
    }

    /**
     * Update employee
     */
    public EmployeeResponseDTO updateEmployee(Integer employeeId, EmployeeRequestDTO dto)
            throws ServiceException {
        try {
            // Check if employee exists
            Optional<Employee> existingOpt = employeeDAO.findById(employeeId);
            if (existingOpt.isEmpty()) {
                throw new ServiceException("Employee not found");
            }

            Employee existing = existingOpt.get();

            // Validate update data
            validateEmployeeRequest(dto, employeeId);

            // Update fields
            existing.setFullName(dto.getFullName());
            existing.setDob(parseDate(dto.getDob()));
            existing.setPhoneNumber(dto.getPhoneNumber());
            existing.setEmail(dto.getEmail());
            existing.setCitizenNumber(dto.getCitizenNumber());
            existing.setHireDate(parseDate(dto.getHireDate()));

            if (dto.getSalary() != null && !dto.getSalary().trim().isEmpty()) {
                existing.setSalary(new BigDecimal(dto.getSalary()));
            }

            employeeDAO.update(existing);

            logger.info("Employee updated: ID={}", employeeId);

            Optional<Account> accountOpt = accountDAO.findById(existing.getAccountId());
            return toResponseDTO(existing, accountOpt.orElse(null));

        } catch (SQLException e) {
            logger.error("Error updating employee: ID={}", employeeId, e);
            throw new ServiceException("Failed to update employee", e);
        }
    }

    /**
     * Deactivate employee (soft delete)
     */
    public void deactivateEmployee(Integer employeeId) throws ServiceException {
        try {
            Optional<Employee> employeeOpt = employeeDAO.findById(employeeId);
            if (employeeOpt.isEmpty()) {
                throw new ServiceException("Employee not found");
            }

            Employee employee = employeeOpt.get();
            employee.setActive(false);
            employeeDAO.update(employee);

            // Also deactivate the associated account
            Optional<Account> accountOpt = accountDAO.findById(employee.getAccountId());
            if (accountOpt.isPresent()) {
                Account account = accountOpt.get();
                account.setActive(false);
                accountDAO.update(account);
            }

            logger.info("Employee deactivated: ID={}", employeeId);

        } catch (SQLException e) {
            logger.error("Error deactivating employee: ID={}", employeeId, e);
            throw new ServiceException("Failed to deactivate employee", e);
        }
    }

    /**
     * Activate employee
     */
    public void activateEmployee(Integer employeeId) throws ServiceException {
        try {
            Optional<Employee> employeeOpt = employeeDAO.findById(employeeId);
            if (employeeOpt.isEmpty()) {
                throw new ServiceException("Employee not found");
            }

            Employee employee = employeeOpt.get();
            employee.setActive(true);
            employeeDAO.update(employee);

            // Also activate the associated account
            Optional<Account> accountOpt = accountDAO.findById(employee.getAccountId());
            if (accountOpt.isPresent()) {
                Account account = accountOpt.get();
                account.setActive(true);
                accountDAO.update(account);
            }

            logger.info("Employee activated: ID={}", employeeId);

        } catch (SQLException e) {
            logger.error("Error activating employee: ID={}", employeeId, e);
            throw new ServiceException("Failed to activate employee", e);
        }
    }

    /**
     * Delete employee permanently (hard delete)
     */
    public void deleteEmployee(Integer employeeId) throws ServiceException {
        try {
            Optional<Employee> employeeOpt = employeeDAO.findById(employeeId);
            if (employeeOpt.isEmpty()) {
                throw new ServiceException("Employee not found");
            }

            boolean deleted = employeeDAO.delete(employeeId);
            if (!deleted) {
                throw new ServiceException("Employee cannot be deleted. May have related records.");
            }

            logger.info("Employee deleted: ID={}", employeeId);

        } catch (SQLException e) {
            logger.error("Error deleting employee: ID={}", employeeId, e);
            throw new ServiceException("Failed to delete employee. Employee may have related contracts.", e);
        }
    }

    // Helper methods
    private void validateEmployeeRequest(EmployeeRequestDTO dto, Integer excludeEmployeeId)
            throws ServiceException, SQLException {

        if (dto.getFullName() == null || dto.getFullName().trim().isEmpty()) {
            throw new ServiceException("Full name is required");
        }

        if (dto.getFullName().length() > 100) {
            throw new ServiceException("Full name cannot exceed 100 characters");
        }

        // Validate phone number
        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().trim().isEmpty()) {
            if (!dto.getPhoneNumber().matches("^[0-9+\\-\\s()]{10,15}$")) {
                throw new ServiceException("Invalid phone number format");
            }

            if (employeeDAO.isPhoneNumberExists(dto.getPhoneNumber(), excludeEmployeeId)) {
                throw new ServiceException("Phone number already exists");
            }
        }

        // Validate email
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            if (!dto.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new ServiceException("Invalid email format");
            }

            if (employeeDAO.isEmailExists(dto.getEmail(), excludeEmployeeId)) {
                throw new ServiceException("Email already exists");
            }
        }

        // Validate citizen number
        if (dto.getCitizenNumber() != null && !dto.getCitizenNumber().trim().isEmpty()) {
            if (dto.getCitizenNumber().length() > 20) {
                throw new ServiceException("Citizen number cannot exceed 20 characters");
            }

            if (employeeDAO.isCitizenNumberExists(dto.getCitizenNumber(), excludeEmployeeId)) {
                throw new ServiceException("Citizen number already exists");
            }
        }

        // Validate dates
        if (dto.getDob() != null && !dto.getDob().trim().isEmpty()) {
            LocalDate dob = parseDate(dto.getDob());
            if (dob.isAfter(LocalDate.now())) {
                throw new ServiceException("Date of birth cannot be in the future");
            }
            if (dob.isBefore(LocalDate.now().minusYears(100))) {
                throw new ServiceException("Invalid date of birth");
            }
        }

        if (dto.getHireDate() != null && !dto.getHireDate().trim().isEmpty()) {
            LocalDate hireDate = parseDate(dto.getHireDate());
            if (hireDate.isAfter(LocalDate.now())) {
                throw new ServiceException("Hire date cannot be in the future");
            }
        }

        // Validate salary
        if (dto.getSalary() != null && !dto.getSalary().trim().isEmpty()) {
            try {
                BigDecimal salary = new BigDecimal(dto.getSalary());
                if (salary.compareTo(BigDecimal.ZERO) < 0) {
                    throw new ServiceException("Salary cannot be negative");
                }
                if (salary.compareTo(new BigDecimal("999999999999.99")) > 0) {
                    throw new ServiceException("Salary value is too large");
                }
            } catch (NumberFormatException e) {
                throw new ServiceException("Invalid salary format");
            }
        }
    }

    private Employee mapDtoToEmployee(EmployeeRequestDTO dto) throws ServiceException {
        Employee employee = new Employee();
        employee.setFullName(dto.getFullName());
        employee.setDob(parseDate(dto.getDob()));
        employee.setPhoneNumber(dto.getPhoneNumber());
        employee.setEmail(dto.getEmail());
        employee.setCitizenNumber(dto.getCitizenNumber());
        employee.setHireDate(parseDate(dto.getHireDate()));

        if (dto.getSalary() != null && !dto.getSalary().trim().isEmpty()) {
            employee.setSalary(new BigDecimal(dto.getSalary()));
        }

        return employee;
    }

    private LocalDate parseDate(String dateStr) throws ServiceException {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ServiceException("Invalid date format. Expected: yyyy-MM-dd");
        }
    }

    private EmployeeResponseDTO toResponseDTO(Employee employee) {
        EmployeeResponseDTO dto = new EmployeeResponseDTO();
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setAccountId(employee.getAccountId());
        dto.setUsername(employee.getUsername());
        dto.setRole(employee.getRole());
        dto.setFullName(employee.getFullName());
        dto.setDob(employee.getDob());
        dto.setPhoneNumber(employee.getPhoneNumber());
        dto.setSalary(employee.getSalary());
        dto.setEmail(employee.getEmail());
        dto.setCitizenNumber(employee.getCitizenNumber());
        dto.setHireDate(employee.getHireDate());
        dto.setActive(employee.isActive());
        dto.setCreatedAt(employee.getCreatedAt());
        dto.setUpdatedAt(employee.getUpdatedAt());
        return dto;
    }

    private EmployeeResponseDTO toResponseDTO(Employee employee, Account account) {
        EmployeeResponseDTO dto = toResponseDTO(employee);
        if (account != null) {
            dto.setUsername(account.getUsername());
            dto.setRole(account.getRole().getValue());
        }
        return dto;
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