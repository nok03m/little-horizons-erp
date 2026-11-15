package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ApplicationRole;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/** Persistence operations for console accounts. */
public class UserAccountDao {

    public Optional<StoredUserAccount> findActiveByUsername(String username) {
        String sql = "SELECT nombre_usuario, hash_contrasena, rol, failed_attempts, "
                + "(locked_until IS NOT NULL AND locked_until > UTC_TIMESTAMP()) AS login_locked "
                + "FROM usuarios_app "
                + "WHERE nombre_usuario = ? AND activo = TRUE";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(new StoredUserAccount(
                            result.getString("nombre_usuario"),
                            result.getString("hash_contrasena"),
                            ApplicationRole.valueOf(result.getString("rol")),
                            result.getInt("failed_attempts"),
                            result.getBoolean("login_locked")));
                }
            }
        } catch (SQLException | IllegalArgumentException exception) {
            AppLogger.error("Failed to find application account", exception);
            throw new IllegalStateException("Unable to authenticate account.", exception);
        }
        return Optional.empty();
    }

    public int countAccounts() {
        String sql = "SELECT COUNT(*) FROM usuarios_app";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        } catch (SQLException exception) {
            AppLogger.error("Failed to count application accounts", exception);
            throw new IllegalStateException("Unable to inspect application accounts.", exception);
        }
    }

    public void recordFailedAttempt(String username) {
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                int failures;
                boolean locked;
                boolean lockExpired;
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT failed_attempts, (locked_until IS NOT NULL AND locked_until > UTC_TIMESTAMP()) AS login_locked, "
                                + "(locked_until IS NOT NULL AND locked_until <= UTC_TIMESTAMP()) AS lock_expired "
                                + "FROM usuarios_app WHERE nombre_usuario=? AND activo=TRUE FOR UPDATE")) {
                    select.setString(1, username);
                    try (ResultSet result = select.executeQuery()) {
                        if (!result.next()) { connection.commit(); return; }
                        failures = result.getInt("failed_attempts");
                        locked = result.getBoolean("login_locked");
                        lockExpired = result.getBoolean("lock_expired");
                    }
                }
                if (locked) { connection.commit(); return; }
                if (lockExpired) failures = 0;
                failures++;
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE usuarios_app SET failed_attempts=?, locked_until=IF(?, DATE_ADD(UTC_TIMESTAMP(), INTERVAL 15 MINUTE), NULL) WHERE nombre_usuario=?")) {
                    update.setInt(1, failures);
                    update.setBoolean(2, failures >= 5);
                    update.setString(3, username);
                    update.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException rollbackException) { exception.addSuppressed(rollbackException); }
                throw exception;
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to record failed login", exception);
            throw new IllegalStateException("Unable to update login protection state.", exception);
        }
    }

    public void recordSuccessfulLogin(String username) {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE usuarios_app SET failed_attempts = 0, locked_until = NULL WHERE nombre_usuario = ?")) {
            statement.setString(1, username);
            statement.executeUpdate();
        } catch (SQLException exception) {
            AppLogger.error("Failed to clear login failure counter", exception);
            throw new IllegalStateException("Unable to reset login protection state.", exception);
        }
    }

    public boolean setActive(String username, boolean active) {
        String lockSql = "SELECT rol FROM usuarios_app WHERE nombre_usuario = ? FOR UPDATE";
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement lifecycleLock = connection.prepareStatement(
                        "SELECT GET_LOCK(CONCAT('lh_account_lifecycle_', LEFT(SHA2(DATABASE(), 256), 16)), 10)")) {
                    try (ResultSet result = lifecycleLock.executeQuery()) {
                        if (!result.next() || result.getInt(1) != 1) {
                            throw new IllegalStateException("Unable to acquire the account lifecycle lock.");
                        }
                    }
                }
                String role;
                try (PreparedStatement lock = connection.prepareStatement(lockSql)) {
                    lock.setString(1, username);
                    try (ResultSet result = lock.executeQuery()) {
                        if (!result.next()) return false;
                        role = result.getString("rol");
                    }
                }
                if (!active && "ADMIN".equals(role)) {
                    try (PreparedStatement count = connection.prepareStatement(
                            "SELECT COUNT(*) FROM usuarios_app WHERE rol='ADMIN' AND activo=TRUE AND nombre_usuario<>?")) {
                        count.setString(1, username);
                        try (ResultSet result = count.executeQuery()) {
                            result.next();
                            if (result.getInt(1) == 0) throw new IllegalStateException("Cannot disable the last active administrator.");
                        }
                    }
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE usuarios_app SET activo=?, failed_attempts=0, locked_until=NULL WHERE nombre_usuario=?")) {
                    update.setBoolean(1, active);
                    update.setString(2, username);
                    boolean changed = update.executeUpdate() > 0;
                    connection.commit();
                    return changed;
                }
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException rollbackException) { exception.addSuppressed(rollbackException); }
                throw exception;
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to change application account status", exception);
            throw new IllegalStateException("Unable to change account status.", exception);
        }
    }

    public int insert(String username, String passwordHash, ApplicationRole role) {
        String sql = "INSERT INTO usuarios_app (nombre_usuario, hash_contrasena, rol) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, role.name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to create application account", exception);
            throw new IllegalStateException("Unable to create account. The username may already exist.", exception);
        }
        throw new IllegalStateException("Account insert did not return an id.");
    }

    public boolean insertFirstAdministrator(String username, String passwordHash) {
        String lockSql = "SELECT GET_LOCK(CONCAT('lh_admin_', LEFT(SHA2(DATABASE(), 256), 16)), 10)";
        String countSql = "SELECT COUNT(*) FROM usuarios_app";
        String insertSql = "INSERT INTO usuarios_app (nombre_usuario, hash_contrasena, rol) VALUES (?, ?, 'ADMIN')";
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement lockStatement = connection.prepareStatement(lockSql);
                 ResultSet lockResult = lockStatement.executeQuery()) {
                if (!lockResult.next() || lockResult.getInt(1) != 1) {
                    throw new IllegalStateException("Unable to acquire the administrator bootstrap lock.");
                }
            }

            connection.setAutoCommit(false);
            try (PreparedStatement countStatement = connection.prepareStatement(countSql);
                 ResultSet countResult = countStatement.executeQuery()) {
                countResult.next();
                if (countResult.getInt(1) != 0) {
                    connection.rollback();
                    return false;
                }
            }

            try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                insertStatement.setString(1, username);
                insertStatement.setString(2, passwordHash);
                insertStatement.executeUpdate();
            }
            connection.commit();
            return true;
        } catch (SQLException exception) {
            AppLogger.error("Failed to bootstrap administrator account", exception);
            throw new IllegalStateException("Unable to bootstrap administrator account.", exception);
        }
    }

    public record StoredUserAccount(String username, String passwordHash, ApplicationRole role,
                                    int failedAttempts, boolean locked) {
    }
}
