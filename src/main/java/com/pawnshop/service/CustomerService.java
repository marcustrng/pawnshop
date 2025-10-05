package com.pawnshop.service;

import com.pawnshop.dao.AccountDAO;
import com.pawnshop.dao.CustomerDAO;
import com.pawnshop.dto.CustomerRequestDTO;
import com.pawnshop.dto.CustomerResponseDTO;
import com.pawnshop.model.Account;
import com.pawnshop.model.Customer;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CustomerService {
    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);
    private final CustomerDAO customerDAO;
    private final AccountDAO accountDAO;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public CustomerService() {
        this.customerDAO = new CustomerDAO();
        this.accountDAO = new AccountDAO();
    }

    public CustomerService(CustomerDAO customerDAO, AccountDAO accountDAO) {
        this.customerDAO = customerDAO;
        this.accountDAO = accountDAO;
    }

    public CustomerResponseDTO createCustomer(CustomerRequestDTO dto) throws ServiceException {
        try {
            validateCustomerRequest(dto, null);

            if (dto.getAccountId() == null) {
                throw new ServiceException("Account ID is required");
            }

            Optional<Account> accountOpt = accountDAO.findById(dto.getAccountId());
            if (accountOpt.isEmpty()) {
                throw new ServiceException("Account not found");
            }

            Account account = accountOpt.get();
            if (account.getRole() != Account.Role.CUSTOMER) {
                throw new ServiceException("Account must have customer role");
            }

            Optional<Customer> existingCustomer = customerDAO.findByAccountId(dto.getAccountId());
            if (existingCustomer.isPresent()) {
                throw new ServiceException("Customer profile already exists for this account");
            }

            Customer customer = mapDtoToCustomer(dto);
            customer.setAccountId(dto.getAccountId());

            Integer customerId = customerDAO.create(customer);
            customer.setCustomerId(customerId);

            logger.info("Customer created successfully: ID={}, account={}",
                    customerId, account.getUsername());

            return toResponseDTO(customer, account);

        } catch (SQLException e) {
            logger.error("Database error creating customer", e);
            throw new ServiceException("Failed to create customer", e);
        }
    }

    public CustomerResponseDTO createCustomerWithAccount(CustomerRequestDTO dto) throws ServiceException {
        try {
            validateCustomerRequest(dto, null);

            if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
                throw new ServiceException("Username is required");
            }
            if (dto.getPassword() == null || dto.getPassword().length() < 6) {
                throw new ServiceException("Password must be at least 6 characters");
            }

            if (accountDAO.isUsernameExists(dto.getUsername(), null)) {
                throw new ServiceException("Username already exists");
            }

            Account account = new Account();
            account.setUsername(dto.getUsername());
            account.setPasswordHash(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(12)));
            account.setRole(Account.Role.CUSTOMER);
            account.setActive(true);

            Integer accountId = accountDAO.create(account);
            account.setAccountId(accountId);

            Customer customer = mapDtoToCustomer(dto);
            customer.setAccountId(accountId);

            Integer customerId = customerDAO.create(customer);
            customer.setCustomerId(customerId);

            logger.info("Customer with account created: customerId={}, username={}",
                    customerId, dto.getUsername());

            return toResponseDTO(customer, account);

        } catch (SQLException e) {
            logger.error("Database error creating customer with account", e);
            throw new ServiceException("Failed to create customer with account", e);
        }
    }

    public CustomerResponseDTO getCustomerById(Integer customerId) throws ServiceException {
        try {
            Optional<Customer> customerOpt = customerDAO.findById(customerId);
            if (customerOpt.isEmpty()) {
                throw new ServiceException("Customer not found");
            }

            Customer customer = customerOpt.get();
            Optional<Account> accountOpt = accountDAO.findById(customer.getAccountId());

            CustomerResponseDTO dto = toResponseDTO(customer, accountOpt.orElse(null));

            // Add contract count
            int contractCount = customerDAO.countContracts(customerId);
            dto.setContractCount(contractCount);

            return dto;

        } catch (SQLException e) {
            logger.error("Error fetching customer: ID={}", customerId, e);
            throw new ServiceException("Failed to fetch customer", e);
        }
    }

    public List<CustomerResponseDTO> getAllCustomers() throws ServiceException {
        try {
            List<Customer> customers = customerDAO.findAll();
            return customers.stream()
                    .map(this::toResponseDTOWithContracts)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            logger.error("Error fetching all customers", e);
            throw new ServiceException("Failed to fetch customers", e);
        }
    }

    public List<CustomerResponseDTO> getActiveCustomers() throws ServiceException {
        try {
            List<Customer> customers = customerDAO.findActive();
            return customers.stream()
                    .map(this::toResponseDTOWithContracts)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            logger.error("Error fetching active customers", e);
            throw new ServiceException("Failed to fetch active customers", e);
        }
    }

    public List<CustomerResponseDTO> searchCustomers(String keyword) throws ServiceException {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return getAllCustomers();
            }

            List<Customer> customers = customerDAO.search(keyword.trim());
            return customers.stream()
                    .map(this::toResponseDTOWithContracts)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            logger.error("Error searching customers", e);
            throw new ServiceException("Failed to search customers", e);
        }
    }

    public CustomerResponseDTO updateCustomer(Integer customerId, CustomerRequestDTO dto)
            throws ServiceException {
        try {
            Optional<Customer> existingOpt = customerDAO.findById(customerId);
            if (existingOpt.isEmpty()) {
                throw new ServiceException("Customer not found");
            }

            Customer existing = existingOpt.get();

            validateCustomerRequest(dto, customerId);

            existing.setFullName(dto.getFullName());
            existing.setCitizenNumber(dto.getCitizenNumber());
            existing.setPhoneNumber(dto.getPhoneNumber());
            existing.setAddress(dto.getAddress());
            existing.setEmail(dto.getEmail());
            existing.setDob(parseDate(dto.getDob()));

            customerDAO.update(existing);

            logger.info("Customer updated: ID={}", customerId);

            Optional<Account> accountOpt = accountDAO.findById(existing.getAccountId());
            return toResponseDTO(existing, accountOpt.orElse(null));

        } catch (SQLException e) {
            logger.error("Error updating customer: ID={}", customerId, e);
            throw new ServiceException("Failed to update customer", e);
        }
    }

    public void deleteCustomer(Integer customerId) throws ServiceException {
        try {
            int contractCount = customerDAO.countContracts(customerId);
            if (contractCount > 0) {
                throw new ServiceException("Cannot delete customer with existing contracts");
            }

            boolean deleted = customerDAO.delete(customerId);
            if (!deleted) {
                throw new ServiceException("Customer not found or cannot be deleted");
            }

            logger.info("Customer deleted: ID={}", customerId);

        } catch (SQLException e) {
            logger.error("Error deleting customer: ID={}", customerId, e);
            throw new ServiceException("Failed to delete customer", e);
        }
    }

    private void validateCustomerRequest(CustomerRequestDTO dto, Integer excludeCustomerId)
            throws ServiceException, SQLException {

        if (dto.getFullName() == null || dto.getFullName().trim().isEmpty()) {
            throw new ServiceException("Full name is required");
        }

        if (dto.getFullName().length() > 100) {
            throw new ServiceException("Full name cannot exceed 100 characters");
        }

        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().trim().isEmpty()) {
            if (!dto.getPhoneNumber().matches("^[0-9+\\-\\s()]{10,15}$")) {
                throw new ServiceException("Invalid phone number format");
            }

            if (customerDAO.isPhoneNumberExists(dto.getPhoneNumber(), excludeCustomerId)) {
                throw new ServiceException("Phone number already exists");
            }
        }

        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            if (!dto.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new ServiceException("Invalid email format");
            }
        }

        if (dto.getCitizenNumber() != null && !dto.getCitizenNumber().trim().isEmpty()) {
            if (dto.getCitizenNumber().length() > 20) {
                throw new ServiceException("Citizen number cannot exceed 20 characters");
            }

            if (customerDAO.isCitizenNumberExists(dto.getCitizenNumber(), excludeCustomerId)) {
                throw new ServiceException("Citizen number already exists");
            }
        }

        if (dto.getDob() != null && !dto.getDob().trim().isEmpty()) {
            LocalDate dob = parseDate(dto.getDob());
            if (dob.isAfter(LocalDate.now())) {
                throw new ServiceException("Date of birth cannot be in the future");
            }
            if (dob.isBefore(LocalDate.now().minusYears(150))) {
                throw new ServiceException("Invalid date of birth");
            }
        }
    }

    private Customer mapDtoToCustomer(CustomerRequestDTO dto) throws ServiceException {
        Customer customer = new Customer();
        customer.setFullName(dto.getFullName());
        customer.setCitizenNumber(dto.getCitizenNumber());
        customer.setPhoneNumber(dto.getPhoneNumber());
        customer.setAddress(dto.getAddress());
        customer.setEmail(dto.getEmail());
        customer.setDob(parseDate(dto.getDob()));
        return customer;
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

    private CustomerResponseDTO toResponseDTO(Customer customer) {
        CustomerResponseDTO dto = new CustomerResponseDTO();
        dto.setCustomerId(customer.getCustomerId());
        dto.setAccountId(customer.getAccountId());
        dto.setUsername(customer.getUsername());
        dto.setRole(customer.getRole());
        dto.setActive(customer.isActive());
        dto.setFullName(customer.getFullName());
        dto.setCitizenNumber(customer.getCitizenNumber());
        dto.setPhoneNumber(customer.getPhoneNumber());
        dto.setAddress(customer.getAddress());
        dto.setEmail(customer.getEmail());
        dto.setDob(customer.getDob());
        dto.setCreatedAt(customer.getCreatedAt());
        dto.setUpdatedAt(customer.getUpdatedAt());
        return dto;
    }

    private CustomerResponseDTO toResponseDTO(Customer customer, Account account) {
        CustomerResponseDTO dto = toResponseDTO(customer);
        if (account != null) {
            dto.setUsername(account.getUsername());
            dto.setRole(account.getRole().getValue());
            dto.setActive(account.isActive());
        }
        return dto;
    }

    private CustomerResponseDTO toResponseDTOWithContracts(Customer customer) {
        CustomerResponseDTO dto = toResponseDTO(customer);
        try {
            int contractCount = customerDAO.countContracts(customer.getCustomerId());
            dto.setContractCount(contractCount);
        } catch (SQLException e) {
            logger.warn("Failed to get contract count for customer: {}", customer.getCustomerId());
            dto.setContractCount(0);
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
