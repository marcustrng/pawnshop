package com.pawnshop.dao;

import com.pawnshop.config.DatabaseConfig;
import com.pawnshop.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccountDAO {
    private static final Logger logger = LoggerFactory.getLogger(AccountDAO.class);

    private static final String INSERT_ACCOUNT =
            "INSERT INTO account (username, password_hash, role, is_active) VALUES (?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "SELECT * FROM account WHERE account_id = ?";

    private static final String SELECT_BY_USERNAME =
            "SELECT * FROM account WHERE username = ?";

    private static final String SELECT_ALL =
            "SELECT * FROM account ORDER BY created_at DESC";

    private static final String UPDATE_ACCOUNT =
            "UPDATE account SET username = ?, role = ?, is_active = ? WHERE account_id = ?";

    private static final String UPDATE_PASSWORD =
            "UPDATE account SET password_hash = ? WHERE account_id = ?";

    private static final String DELETE_ACCOUNT =
            "DELETE FROM account WHERE account_id = ?";

    private static final String CHECK_USERNAME_EXISTS =
            "SELECT COUNT(*) FROM account WHERE username = ? AND account_id != ?";

    public Integer create(Account account) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_ACCOUNT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, account.getUsername());
            stmt.setString(2, account.getPasswordHash());
            stmt.setString(3, account.getRole().getValue());
            stmt.setBoolean(4, account.isActive());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating account failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Integer accountId = generatedKeys.getInt(1);
                    account.setAccountId(accountId);
                    logger.info("Account created successfully with ID: {}", accountId);
                    return accountId;
                } else {
                    throw new SQLException("Creating account failed, no ID obtained.");
                }
            }
        }
    }

    public Optional<Account> findById(Integer accountId) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID)) {

            stmt.setInt(1, accountId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAccount(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Account> findByUsername(String username) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_USERNAME)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAccount(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Account> findAll() throws SQLException {
        List<Account> accounts = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                accounts.add(mapResultSetToAccount(rs));
            }
        }

        return accounts;
    }

    public boolean update(Account account) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_ACCOUNT)) {

            stmt.setString(1, account.getUsername());
            stmt.setString(2, account.getRole().getValue());
            stmt.setBoolean(3, account.isActive());
            stmt.setInt(4, account.getAccountId());

            int affectedRows = stmt.executeUpdate();
            logger.info("Account updated: ID={}, affected rows={}", account.getAccountId(), affectedRows);
            return affectedRows > 0;
        }
    }

    public boolean updatePassword(Integer accountId, String newPasswordHash) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_PASSWORD)) {

            stmt.setString(1, newPasswordHash);
            stmt.setInt(2, accountId);

            int affectedRows = stmt.executeUpdate();
            logger.info("Password updated for account ID: {}", accountId);
            return affectedRows > 0;
        }
    }

    public boolean delete(Integer accountId) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_ACCOUNT)) {

            stmt.setInt(1, accountId);
            int affectedRows = stmt.executeUpdate();
            logger.info("Account deleted: ID={}, affected rows={}", accountId, affectedRows);
            return affectedRows > 0;
        }
    }

    public boolean isUsernameExists(String username, Integer excludeAccountId) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(CHECK_USERNAME_EXISTS)) {

            stmt.setString(1, username);
            stmt.setInt(2, excludeAccountId != null ? excludeAccountId : -1);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setAccountId(rs.getInt("account_id"));
        account.setUsername(rs.getString("username"));
        account.setPasswordHash(rs.getString("password_hash"));
        account.setRole(Account.Role.fromString(rs.getString("role")));
        account.setActive(rs.getBoolean("is_active"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            account.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            account.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return account;
    }
}