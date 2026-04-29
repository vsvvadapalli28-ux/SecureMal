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
import java.util.concurrent.TimeUnit;

public class AnalysisController {
    
    /**
     * Returns the project root directory — the folder containing the JAR or,
     * during development, the Maven working directory.
     */
    private static File getProjectRoot() {
        try {
            // When running as a JAR, locate the JAR and use its parent as root
            java.net.URL location = AnalysisController.class
                    .getProtectionDomain().getCodeSource().getLocation();
            File jarFile = new File(location.toURI());
            File parent = jarFile.getParentFile(); // target/
            if (parent != null && parent.getParentFile() != null) {
                return parent.getParentFile(); // project root
            }
        } catch (Exception ignored) {}
        // Fallback: use the JVM's current working directory
        return new File(System.getProperty("user.dir"));
    }

    public static String runScript(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        File projectRoot = getProjectRoot();
        pb.directory(projectRoot);
        System.out.println("[AnalysisController] Working dir: " + projectRoot.getAbsolutePath());
        System.out.println("[AnalysisController] Command: " + String.join(" ", command));
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

        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new RuntimeException("Script execution timed out after 30 seconds");
        }

        String stderr = errors.toString().trim();
        if (!stderr.isEmpty()) {
            System.err.println("[Python stderr]: " + stderr);
            if (stderr.contains("Could not find a registered machine")) {
                throw new RuntimeException("Could not find a registered machine");
            }
        }

        String result = output.toString().trim();
        System.out.println("[AnalysisController] Output length: " + result.length() + " chars");
        if (result.isEmpty()) {
            throw new RuntimeException("Python script returned empty output. stderr: " + stderr);
        }
        return result;
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
            AnalysisReport report = reportService.saveReport(fileId, jsonResult);
            if (report == null) {
                throw new RuntimeException("Report service failed to save analysis output for file ID " + fileId);
            }
            return report;
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
                AnalysisReport report = reportService.saveReport(fileId, rootObj.toString());
                if (report == null) {
                    throw new RuntimeException("Report service failed to save dynamic analysis output for file ID " + fileId);
                }
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
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("Could not find a registered machine")) {
                        javax.swing.JOptionPane.showMessageDialog(dashboard, "Dynamic Analysis requires a VirtualBox VM named 'SecureMal-Clean' with a snapshot named 'Clean'. Please set this up in VirtualBox first. Static analysis results are still available.", "Dynamic Analysis Unavailable", javax.swing.JOptionPane.WARNING_MESSAGE);
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(dashboard, "Dynamic Analysis failed: " + msg, "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                    }
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
                    // Refresh table immediately on the EDT before showing dialog
                    javax.swing.SwingUtilities.invokeLater(() -> dashboard.refreshTable());
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
