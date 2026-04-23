package com.securemal.db;

import com.securemal.config.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Thread-safe singleton for Database connection.
 */
public class DBConnection {

    private static DBConnection instance;
    private Connection connection;

    private DBConnection() {
        try {
            this.connection = DriverManager.getConnection(Config.DB_URL, Config.DB_USER, Config.DB_PASS);
        } catch (SQLException e) {
            throw new RuntimeException("Error initializing database connection. Ensure MySQL is running.", e);
        }
    }

    /**
     * Retrieves the instance of DBConnection in a thread-safe manner.
     * @return DBConnection instance
     */
    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        } else {
            try {
                if (instance.connection == null || instance.connection.isClosed()) {
                    instance = new DBConnection();
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error checking DB connection status", e);
            }
        }
        return instance;
    }

    /**
     * Gets the current SQL Connection.
     * @return SQL Connection
     */
    public Connection getConnection() {
        return this.connection;
    }
}
