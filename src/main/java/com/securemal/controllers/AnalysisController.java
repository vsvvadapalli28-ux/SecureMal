package com.securemal.controllers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.securemal.config.Config;
import com.securemal.db.DBConnection;
import com.securemal.db.DBHelper;
import com.securemal.models.AnalysisReport;
import com.securemal.services.ReportService;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AnalysisController {
    
    public static String runScript(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File("."));
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        StringBuilder errors = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                errors.append(line).append("\n");
            }
        }

        process.waitFor();
        if (errors.length() > 0) {
            System.err.println("Python stderr: " + errors.toString());
        }
        return output.toString().trim();
    }

    public static String getFilePath(int fileId) {
        String filePath = null;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getInstance().getConnection();
            String sql = "SELECT file_path FROM files WHERE id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, fileId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                filePath = rs.getString("file_path");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Database error retrieving file path.");
        } finally {
            DBHelper.closeQuietly(rs);
            DBHelper.closeQuietly(pstmt);
        }
        if (filePath == null) {
            throw new RuntimeException("File not found for ID: " + fileId);
        }
        return filePath;
    }

    public static AnalysisReport runStaticAnalysis(int fileId) {
        String filePath = getFilePath(fileId);
        try {
            String jsonResult = runScript(Config.PYTHON_CMD, Config.ANALYZER_SCRIPT, "--file", filePath);
            if (jsonResult.isEmpty()) throw new RuntimeException("Python engine returned blank output.");
            JsonObject rootObj = JsonParser.parseString(jsonResult).getAsJsonObject();
            if (rootObj.has("error")) throw new RuntimeException("Python error: " + rootObj.get("error").getAsString());

            ReportService reportService = new ReportService();
            return reportService.saveReport(fileId, jsonResult);
        } catch (Exception e) {
            throw new RuntimeException("Failed to run python analyzer.", e);
        }
    }

    public static void runDynamicAnalysis(int fileId, com.securemal.ui.DashboardScreen dashboard) {
        javax.swing.SwingWorker<Void, Void> worker = new javax.swing.SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                String filePath = getFilePath(fileId);
                
                // 1. Static base
                String staticJson = runScript(Config.PYTHON_CMD, Config.ANALYZER_SCRIPT, "--file", filePath);
                JsonObject rootObj = JsonParser.parseString(staticJson).getAsJsonObject();
                if (rootObj.has("error")) throw new RuntimeException(rootObj.get("error").getAsString());
                
                // 2. Execute VM
                String execJson = runScript(Config.PYTHON_CMD, "sandbox/executor.py", "--file", filePath, "--vm", "SecureMal-Clean", "--snapshot", "Clean");
                JsonObject execObj = JsonParser.parseString(execJson).getAsJsonObject();
                if (execObj.has("error")) throw new RuntimeException(execObj.get("error").getAsString());
                String sysmonLog = execObj.get("sysmon_log").getAsString();
                
                // 3. Parse Sysmon
                String parserJson = runScript(Config.PYTHON_CMD, "sandbox/monitor_parser.py", "--sysmon", sysmonLog, "--output", "reports/dynamic_timeline.json");
                com.google.gson.JsonArray dynamicTimeline = JsonParser.parseString(parserJson).getAsJsonArray();
                
                // 4. Merge
                com.google.gson.JsonArray combinedTimeline = rootObj.getAsJsonArray("timeline");
                combinedTimeline.addAll(dynamicTimeline);
                rootObj.addProperty("analysis_type", "Dynamic Analysis");
                
                ReportService reportService = new ReportService();
                reportService.saveReport(fileId, rootObj.toString());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    dashboard.refreshTable();
                    javax.swing.JOptionPane.showMessageDialog(dashboard, "Dynamic Analysis Complete!", "Success", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    e.printStackTrace();
                    javax.swing.JOptionPane.showMessageDialog(dashboard, "Dynamic Analysis failed: " + e.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // Backwards compatibility for the dashboard
    public static void runStaticAnalysis(int fileId, com.securemal.ui.DashboardScreen dashboard) {
        javax.swing.SwingWorker<Void, Void> worker = new javax.swing.SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                runStaticAnalysis(fileId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    dashboard.refreshTable();
                    javax.swing.JOptionPane.showMessageDialog(dashboard, "Analysis Complete!", "Success", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    e.printStackTrace();
                    javax.swing.JOptionPane.showMessageDialog(dashboard, "Analysis failed: " + e.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
