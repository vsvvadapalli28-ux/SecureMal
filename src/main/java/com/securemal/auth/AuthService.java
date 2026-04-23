package com.securemal.auth;

import com.securemal.db.DBConnection;
import com.securemal.db.DBHelper;
import com.securemal.models.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class AuthService {

    public boolean registerUser(String username, String email, String password) {
        if (username == null || username.trim().isEmpty() || username.length() < 3 || username.length() > 30) {
            return false;
        }
        if (password == null || password.trim().isEmpty() || password.length() < 6) {
            return false;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBConnection.getInstance().getConnection();
            String sql = "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, hashedPassword);

            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DBHelper.closeQuietly(pstmt);
            // Don't close connection here because it's a singleton
        }
    }

    public User loginUser(String username, String password) {
        if (username == null || password == null) return null;

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getInstance().getConnection();
            String sql = "SELECT * FROM users WHERE username = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            
            rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (BCrypt.checkpw(password, storedHash)) {
                    int id = rs.getInt("id");
                    String email = rs.getString("email");
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    return new User(id, username, storedHash, email, createdAt);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBHelper.closeQuietly(rs);
            DBHelper.closeQuietly(pstmt);
        }
        return null;
    }
}
