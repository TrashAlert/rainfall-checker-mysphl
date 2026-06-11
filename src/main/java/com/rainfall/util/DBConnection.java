package com.rainfall.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection — Database Connection Utility
 *
 * Connects to rainfall_db2 (the Malaysia + Philippines database).
 * All Servlets call getConnection() to obtain a JDBC connection.
 */
public class DBConnection {

    private static final String DB_URL  = "jdbc:mysql://localhost:3306/rainfall_db2?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "abc123";

    /**
     * getConnection()
     * Returns an open Connection to rainfall_db2.
     * Caller must close it after use.
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found.", e);
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }
}
