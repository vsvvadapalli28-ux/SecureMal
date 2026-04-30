package com.securemal.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.securemal.config.Config;
import com.securemal.db.DBConnection;
import com.securemal.models.UploadedFile;

public class FileManagementService {

    public UploadedFile uploadFile(File selectedFile, int userId) {
        String originalName = selectedFile.getName();
        String storedFilename = UUID.randomUUID().toString() + "_" + originalName;
        Path targetPath = Paths.get(Config.UPLOADS_DIR, storedFilename).toAbsolutePath();

        try {
            // Copy file physically
            Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            long fileSize = selectedFile.length();
            String filePathStr = targetPath.toString(); // absolute path

            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO files (user_id, original_filename, stored_filename, file_path, file_size) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, originalName);
                pstmt.setString(3, storedFilename);
                pstmt.setString(4, filePathStr);
                pstmt.setLong(5, fileSize);

                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            int newId = rs.getInt(1);
                            UploadedFile uf = new UploadedFile(newId, userId, originalName, storedFilename, filePathStr, fileSize, new Timestamp(System.currentTimeMillis()));
                            uf.setStatus("Pending");
                            uf.setRiskBadge("Pending");
                            return uf;
                        }
                    }
                }
            }
        } catch (IOException | java.sql.SQLException e) {
            System.err.println("Upload failed: " + e.getMessage());
            // Optional: delete copied file if db insert fails
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException ignored) {
                // no-op
            }
        }
        return null;
    }

    public List<UploadedFile> getFilesForUser(int userId) {
        List<UploadedFile> list = new ArrayList<>();
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            // Using a LEFT JOIN to check if a report exists for the file to determine Status & Risk.
            String sql = "SELECT f.*, r.id AS report_id, r.risk_label " +
                         "FROM files f " +
                         "LEFT JOIN reports r ON f.id = r.file_id " +
                         "WHERE f.user_id = ? " +
                         "ORDER BY f.uploaded_at DESC";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        UploadedFile uf = new UploadedFile();
                        uf.setId(rs.getInt("id"));
                        uf.setUserId(rs.getInt("user_id"));
                        uf.setOriginalFilename(rs.getString("original_filename"));
                        uf.setStoredFilename(rs.getString("stored_filename"));
                        uf.setFilePath(rs.getString("file_path"));
                        uf.setFileSize(rs.getLong("file_size"));
                        uf.setUploadedAt(rs.getTimestamp("uploaded_at"));

                        // Status logic based on reports table existence
                        if (rs.getObject("report_id") != null) {
                            uf.setStatus("Analysed");
                            String risk = rs.getString("risk_label");
                            uf.setRiskBadge(risk != null ? risk : "Unknown");
                        } else {
                            uf.setStatus("Pending");
                            uf.setRiskBadge("Pending");
                        }
                        
                        list.add(uf);
                    }
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Failed to fetch user files: " + e.getMessage());
        }
        return list;
    }

    /**
     * Deletes a file record from the database and removes the physical
     * file from the uploads directory. Also removes any associated report.
     *
     * @param fileId the ID of the file to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteFile(int fileId) {
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            // Step 1: Get the stored filename before deleting
            String filePath = null;
            try (PreparedStatement getPath = conn.prepareStatement(
                    "SELECT file_path, stored_filename FROM files WHERE id = ?")) {
                getPath.setInt(1, fileId);
                try (ResultSet rs = getPath.executeQuery()) {
                    if (rs.next()) {
                        filePath = rs.getString("file_path");
                    }
                }
            }

            // Step 2: Delete associated report first (foreign key constraint)
            try (PreparedStatement deleteReport = conn.prepareStatement(
                    "DELETE FROM reports WHERE file_id = ?")) {
                deleteReport.setInt(1, fileId);
                deleteReport.executeUpdate();
            }

            // Step 3: Delete the file record
            int rows = 0;
            try (PreparedStatement deleteFile = conn.prepareStatement(
                    "DELETE FROM files WHERE id = ?")) {
                deleteFile.setInt(1, fileId);
                rows = deleteFile.executeUpdate();
                if (rows <= 0) {
                    return false;
                }
            }

            // Step 4: Delete physical file from disk
            if (filePath != null) {
                File physicalFile = new File(filePath);
                if (physicalFile.exists()) {
                    physicalFile.delete();
                }
            }

            return rows > 0;
        } catch (java.sql.SQLException e) {
            System.err.println("Error deleting file: " + e.getMessage());
            return false;
        }
    }
}
