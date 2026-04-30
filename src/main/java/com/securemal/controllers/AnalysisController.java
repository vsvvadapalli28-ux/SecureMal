package com.securemal.controllers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.securemal.config.AppConfig;
import com.securemal.config.Config;
import com.securemal.db.DBConnection;
import com.securemal.models.AnalysisReport;
import com.securemal.services.ReportService;

import java.awt.Dimension;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

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
        } catch (java.net.URISyntaxException | NullPointerException ignored) {}
        // Fallback: use the JVM's current working directory
        return new File(System.getProperty("user.dir"));
    }

    public static String runScript(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        String vboxManage = AppConfig.getVirtualBoxPath();
        if (vboxManage != null && !vboxManage.isBlank()) {
            pb.environment().put("VBOXMANAGE", vboxManage);
        }
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

    private static List<String> listRegisteredVirtualMachines(String vboxPath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(vboxPath, "list", "vms");
        pb.environment().put("VBOXMANAGE", vboxPath);
        pb.directory(getProjectRoot());
        Process process = pb.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append(System.lineSeparator());
            }
        }

        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new RuntimeException("VBoxManage list vms timed out.");
        }

        List<String> vms = new ArrayList<>();
        for (String line : out.toString().split(System.lineSeparator())) {
            if (line.startsWith("\"")) {
                int endQuote = line.indexOf('"', 1);
                if (endQuote > 1) {
                    vms.add(line.substring(1, endQuote));
                }
            }
        }
        return vms;
    }

    private static void validateVirtualBoxEnvironment(String vmName, String snapshotName) throws IOException, InterruptedException {
        String vboxPath = AppConfig.getVirtualBoxPath();
        if (vboxPath == null || vboxPath.isBlank() || !new File(vboxPath).isFile()) {
            throw new IllegalStateException("VirtualBox path is not configured or is invalid. Set virtualbox.path in securemal.properties or define VBOXMANAGE in your environment.");
        }

        List<String> vms = listRegisteredVirtualMachines(vboxPath);
        if (!vms.contains(vmName)) {
            String available = vms.isEmpty() ? "<none>" : String.join(", ", vms);
            throw new RuntimeException("VirtualBox path is valid, but VM '" + vmName + "' is not registered. Available VMs: " + available);
        }

        ProcessBuilder pb = new ProcessBuilder(vboxPath, "snapshot", vmName, "list");
        pb.environment().put("VBOXMANAGE", vboxPath);
        pb.directory(getProjectRoot());
        Process process = pb.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append(System.lineSeparator());
            }
        }

        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new RuntimeException("VBoxManage snapshot list timed out.");
        }

        String snapshotOutput = out.toString();
        if (!snapshotOutput.contains("\"" + snapshotName + "\"")) {
            throw new RuntimeException("The VirtualBox VM '" + vmName + "' is registered, but snapshot '" + snapshotName + "' was not found. Snapshot list:\n" + snapshotOutput.trim());
        }
    }

    public static String getFilePath(int fileId) {
        String filePath = null;

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT file_path FROM files WHERE id = ?")) {
            pstmt.setInt(1, fileId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    filePath = rs.getString("file_path");
                }
            }
        } catch (SQLException e) {
            System.err.println("[AnalysisController] Failed to retrieve file path: " + e.getMessage());
            throw new RuntimeException("Database error retrieving file path.", e);
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
            if (jsonResult.isEmpty()) {
                throw new RuntimeException("Python engine returned blank output.");
            }
            JsonObject rootObj = JsonParser.parseString(jsonResult).getAsJsonObject();
            if (rootObj.has("error")) {
                throw new RuntimeException("Python error: " + rootObj.get("error").getAsString());
            }

            ReportService reportService = new ReportService();
            AnalysisReport report = reportService.saveReport(fileId, jsonResult);
            if (report == null) {
                throw new RuntimeException("Report service failed to save analysis output for file ID " + fileId);
            }
            return report;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to run python analyzer.", e);
        }
    }

    public static void runDynamicAnalysis(int fileId, com.securemal.ui.DashboardScreen dashboard) {
        javax.swing.SwingWorker<Void, Void> worker = new javax.swing.SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                String vmName = "SecureMal-Clean";
                String snapshotName = "Clean";
                validateVirtualBoxEnvironment(vmName, snapshotName);
                String filePath = getFilePath(fileId);

                // 1. Static base
                String staticJson = runScript(Config.PYTHON_CMD, Config.ANALYZER_SCRIPT, "--file", filePath);
                JsonObject rootObj = JsonParser.parseString(staticJson).getAsJsonObject();
                if (rootObj.has("error")) {
                    throw new RuntimeException(rootObj.get("error").getAsString());
                }

                // 2. Execute VM
                String execJson = runScript(Config.PYTHON_CMD, "sandbox/executor.py", "--file", filePath, "--vm", vmName, "--snapshot", snapshotName);
                JsonObject execObj = JsonParser.parseString(execJson).getAsJsonObject();
                if (execObj.has("error")) {
                    throw new RuntimeException(execObj.get("error").getAsString());
                }
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
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showMessage(dashboard, "Dynamic Analysis was interrupted. Static analysis results are still available.", "Dynamic Analysis Interrupted", JOptionPane.WARNING_MESSAGE);
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof IOException) {
                        showMessage(dashboard, "Dynamic Analysis failed because the required tools could not be executed. "
                                + "Check that Python and VirtualBox are installed and reachable.\n\nDetails: "
                                + cause.getMessage(),
                                "Dynamic Analysis Unavailable", JOptionPane.WARNING_MESSAGE);
                    } else if (cause instanceof RuntimeException) {
                        String msg = cause.getMessage();
                        if (msg != null && msg.contains("not registered")) {
                            showMessage(dashboard, "Dynamic Analysis requires a configured VirtualBox installation and a VM named 'SecureMal-Clean' with a snapshot named 'Clean'.\n\n"
                                    + msg + "\n\nStatic analysis results are still available.",
                                    "Dynamic Analysis Unavailable", JOptionPane.WARNING_MESSAGE);
                        } else if (msg != null && msg.contains("VirtualBox path is not configured")) {
                            showMessage(dashboard, "Dynamic Analysis requires a configured VirtualBox installation. "
                                    + "Please set virtualbox.path in securemal.properties or VBOXMANAGE first.\n\n"
                                    + "Static analysis results are still available.",
                                    "Dynamic Analysis Unavailable", JOptionPane.WARNING_MESSAGE);
                        } else {
                            showMessage(dashboard, "Dynamic Analysis failed: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        showMessage(dashboard, "Dynamic Analysis failed: " + (cause != null ? cause.getMessage() : e.getMessage()), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }

            private void showMessage(JComponent parent, String message, String title, int messageType) {
                JTextArea textArea = new JTextArea(message);
                textArea.setEditable(false);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setBackground(parent.getBackground());
                textArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                textArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(520, 180));
                scrollPane.setBorder(BorderFactory.createEmptyBorder());
                scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

                JOptionPane.showMessageDialog(parent, scrollPane, title, messageType);
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
                } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                    Throwable cause = e.getCause();
                    System.err.println("Static analysis worker failed: " + e.getMessage());
                    javax.swing.JOptionPane.showMessageDialog(dashboard, "Analysis failed: " + (cause != null ? cause.getMessage() : e.getMessage()), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
