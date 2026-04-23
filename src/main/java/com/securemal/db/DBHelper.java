package com.securemal.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Helper class for quiet database resource cleanup.
 */
public class DBHelper {

    /**
     * Closes ResultSet quietly.
     * @param rs ResultSet to close
     */
    public static void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Closes Statement/PreparedStatement quietly.
     * @param stmt Statement to close
     */
    public static void closeQuietly(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Closes PreparedStatement quietly.
     * @param pstmt PreparedStatement to close
     */
    public static void closeQuietly(PreparedStatement pstmt) {
        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Closes Connection quietly.
     * @param conn Connection to close
     */
    public static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception ignored) {}
        }
    }
}
