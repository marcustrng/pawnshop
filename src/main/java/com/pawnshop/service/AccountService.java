package com.pawnshop.service;

import com.pawnshop.dao.AccountDAO;
import com.pawnshop.dto.AccountRegistrationDTO;
import com.pawnshop.dto.AccountResponseDTO;
import com.pawnshop.dto.LoginRequestDTO;
import com.pawnshop.model.Account;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private final AccountDAO accountDAO;

    public AccountService() {
        this.accountDAO = new AccountDAO();
    }

    public AccountService(AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }

    /**
     * Register a new account (Admin only)
     */
    public AccountResponseDTO registerAccount(AccountRegistrationDTO dto) throws ServiceException {
        try {
            // Validate input
            validateRegistration(dto);

            // Check if username already exists
            if (accountDAO.isUsernameExists(dto.getUsername(), null)) {
                throw new ServiceException("Username already exists");
            }

            // Hash password
            String passwordHash = BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(12));

            // Create account
            Account account = new Account();
            account.setUsername(dto.getUsername());
            account.setPasswordHash(passwordHash);
            account.setRole(Account.Role.valueOf(dto.getRole().toUpperCase()));
            account.setActive(true);

            Integer accountId = accountDAO.create(account);
            account.setAccountId(accountId);

            logger.info("Account registered successfully: username={}, role={}",
                    dto.getUsername(), dto.getRole());

            return toResponseDTO(account);

        } catch (SQLException e) {
            logger.error("Database error during registration", e);
            throw new ServiceException("Failed to register account", e);
        }
    }

    /**
     * Authenticate user login
     */
    public AccountResponseDTO login(LoginRequestDTO dto) throws ServiceException {
        try {
            // Validate input
            if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
                throw new ServiceException("Username is required");
            }
            if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
                throw new ServiceException("Password is required");
            }

            // Find account
            Optional<Account> accountOpt = accountDAO.findByUsername(dto.getUsername());

            if (accountOpt.isEmpty()) {
                throw new ServiceException("Invalid username or password");
            }

            Account account = accountOpt.get();

            // Check if account is active
            if (!account.isActive()) {
                throw new ServiceException("Account is inactive");
            }

            // Verify password
            if (!BCrypt.checkpw(dto.getPassword(), account.getPasswordHash())) {
                throw new ServiceException("Invalid username or password");
            }

            logger.info("User logged in successfully: username={}", dto.getUsername());
            return toResponseDTO(account);

        } catch (SQLException e) {
            logger.error("Database error during login", e);
            throw new ServiceException("Login failed", e);
        }
    }

    /**
     * Get account by ID
     */
    public AccountResponseDTO getAccountById(Integer accountId) throws ServiceException {
        try {
            Optional<Account> accountOpt = accountDAO.findById(accountId);
            return accountOpt.map(this::toResponseDTO)
                    .orElseThrow(() -> new ServiceException("Account not found"));
        } catch (SQLException e) {
            logger.error("Error fetching account by ID: {}", accountId, e);
            throw new ServiceException("Failed to fetch account", e);
        }
    }

    /**
     * Get all accounts (Admin only)
     */
    public List<AccountResponseDTO> getAllAccounts() throws ServiceException {
        try {
            List<Account> accounts = accountDAO.findAll();
            return accounts.stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            logger.error("Error fetching all accounts", e);
            throw new ServiceException("Failed to fetch accounts", e);
        }
    }

    /**
     * Update account (Admin only)
     */
    public AccountResponseDTO updateAccount(Integer accountId, AccountRegistrationDTO dto)
            throws ServiceException {
        try {
            // Check if account exists
            Optional<Account> existingOpt = accountDAO.findById(accountId);
            if (existingOpt.isEmpty()) {
                throw new ServiceException("Account not found");
            }

            Account existing = existingOpt.get();

            // Validate username uniqueness if changed
            if (!existing.getUsername().equals(dto.getUsername())) {
                if (accountDAO.isUsernameExists(dto.getUsername(), accountId)) {
                    throw new ServiceException("Username already exists");
                }
            }

            // Update account
            existing.setUsername(dto.getUsername());
            existing.setRole(Account.Role.valueOf(dto.getRole().toUpperCase()));

            // Update password if provided
            if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
                String passwordHash = BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(12));
                accountDAO.updatePassword(accountId, passwordHash);
            }

            accountDAO.update(existing);

            logger.info("Account updated successfully: ID={}", accountId);
            return toResponseDTO(existing);

        } catch (SQLException e) {
            logger.error("Error updating account: ID={}", accountId, e);
            throw new ServiceException("Failed to update account", e);
        }
    }

    /**
     * Deactivate account (Admin only)
     */
    public void deactivateAccount(Integer accountId) throws ServiceException {
        try {
            Optional<Account> accountOpt = accountDAO.findById(accountId);
            if (accountOpt.isEmpty()) {
                throw new ServiceException("Account not found");
            }

            Account account = accountOpt.get();
            account.setActive(false);
            accountDAO.update(account);

            logger.info("Account deactivated: ID={}", accountId);

        } catch (SQLException e) {
            logger.error("Error deactivating account: ID={}", accountId, e);
            throw new ServiceException("Failed to deactivate account", e);
        }
    }

    /**
     * Delete account (Admin only - use with caution)
     */
    public void deleteAccount(Integer accountId) throws ServiceException {
        try {
            boolean deleted = accountDAO.delete(accountId);
            if (!deleted) {
                throw new ServiceException("Account not found or cannot be deleted");
            }
            logger.info("Account deleted: ID={}", accountId);
        } catch (SQLException e) {
            logger.error("Error deleting account: ID={}", accountId, e);
            throw new ServiceException("Failed to delete account. May have dependencies.", e);
        }
    }

    // Helper methods
    private void validateRegistration(AccountRegistrationDTO dto) throws ServiceException {
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            throw new ServiceException("Username is required");
        }
        if (dto.getUsername().length() < 3 || dto.getUsername().length() > 50) {
            throw new ServiceException("Username must be between 3 and 50 characters");
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            throw new ServiceException("Password must be at least 6 characters");
        }
        if (dto.getRole() == null || dto.getRole().trim().isEmpty()) {
            throw new ServiceException("Role is required");
        }

        // Validate role
        try {
            Account.Role.valueOf(dto.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Invalid role. Must be: customer, employee, or admin");
        }
    }

    private AccountResponseDTO toResponseDTO(Account account) {
        AccountResponseDTO dto = new AccountResponseDTO();
        dto.setAccountId(account.getAccountId());
        dto.setUsername(account.getUsername());
        dto.setRole(account.getRole().getValue());
        dto.setActive(account.isActive());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());
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