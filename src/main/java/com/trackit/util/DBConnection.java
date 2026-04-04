package com.trackit.util;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection — JDBC Connection Utility
 *
 * Reads database credentials from the .env file at project root.
 * Uses dotenv-java (io.github.cdimascio:dotenv-java) to load:
 *   DB_URL, DB_USERNAME, DB_PASSWORD
 *
 * Usage:
 *   try (Connection conn = DBConnection.getConnection()) { ... }
 */
public class DBConnection {

    private static final Dotenv dotenv = Dotenv.configure()
            .directory(System.getProperty("user.dir"))  // looks for .env in working directory
            .ignoreIfMissing()                           // won't throw if no .env file (uses OS env vars)
            .load();

    /** JDBC URL — e.g. jdbc:mysql://localhost:3306/trackit */
    private static final String DB_URL      = getEnv("DB_URL");
    /** Database username */
    private static final String DB_USERNAME = getEnv("DB_USERNAME");
    /** Database password */
    private static final String DB_PASSWORD = getEnv("DB_PASSWORD");

    // Private constructor — utility class, not instantiable
    private DBConnection() {}

    /**
     * Returns a fresh JDBC Connection.
     * Caller is responsible for closing it (try-with-resources recommended).
     *
     * @return  active Connection
     * @throws  RuntimeException if the connection cannot be established
     */
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found. Add mysql-connector-j to pom.xml.", e);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Cannot connect to database. Check DB_URL, DB_USERNAME, DB_PASSWORD in .env file. Error: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Reads a required environment variable (from .env or system env).
     * Throws a clear error if the variable is missing.
     */
    private static String getEnv(String key) {
        String value = dotenv.get(key, System.getenv(key));
        if (value == null || value.isBlank()) {
            throw new RuntimeException(
                "Missing required environment variable: " + key +
                ". Please set it in the .env file or as a system environment variable.");
        }
        return value;
    }
}

