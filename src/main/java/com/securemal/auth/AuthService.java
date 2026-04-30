package com.securemal.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.mindrot.jbcrypt.BCrypt;

import com.securemal.db.DBConnection;
import com.securemal.models.User;

public class AuthService {

    /**
     * Registers a new user.
     * @return  1 = success, -1 = email already in use, 0 = other error
     */
    public int registerUser(String username, String email, String password) {
        if (username == null || username.trim().isEmpty() || username.length() < 3 || username.length() > 30) {
            return 0;
        }
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            return 0;
        }
        if (password == null || password.trim().isEmpty() || password.length() < 6) {
            return 0;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        String sql = "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            pstmt.setString(2, email.trim().toLowerCase());
            pstmt.setString(3, hashedPassword);

            int rows = pstmt.executeUpdate();
            return rows > 0 ? 1 : 0;
        } catch (SQLException e) {
            // SQLState 23000 = integrity constraint violation (duplicate email)
            if ("23000".equals(e.getSQLState())) {
                return -1;
            }
            System.err.println("Failed to register user: " + e.getMessage());
            return 0;
        }
    }

    public User loginUser(String email, String password) {
        if (email == null || password == null) return null;

        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (BCrypt.checkpw(password, storedHash)) {
                        int id = rs.getInt("id");
                        String username = rs.getString("username");
                        String userEmail = rs.getString("email");
                        Timestamp createdAt = rs.getTimestamp("created_at");
                        return new User(id, username, storedHash, userEmail, createdAt);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to login user: " + e.getMessage());
        }
        return null;
    }
}
