package com.littlehorizons.erp.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton that provides JDBC connections to MySQL.
 */
public final class DatabaseConnection {

    private static final DatabaseConnection INSTANCE = new DatabaseConnection();

    private final String jdbcUrl;
    private final String username;
    private final String password;

    private DatabaseConnection() {
        this.jdbcUrl = ConfigLoader.buildJdbcUrl();
        this.username = ConfigLoader.getRequired("db.user");
        this.password = ConfigLoader.getRequired("db.password");
    }

    public static DatabaseConnection getInstance() {
        return INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    public boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(3);
        } catch (SQLException exception) {
            AppLogger.error("Database connection test failed", exception);
            return false;
        }
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }
}
