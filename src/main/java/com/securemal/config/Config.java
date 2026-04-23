package com.securemal.config;

import java.awt.Color;
import java.io.File;

/**
 * Global Configuration class for SecureMal.
 * Contains all the constants for database, filesystem, execution paths, and UI colours.
 */
public class Config {

    // Application properties
    public static final String APP_TITLE = "🔒 SecureMal";
    public static final String APP_VERSION = "1.0";

    // DB Constants
    public static final String DB_URL = "jdbc:mysql://localhost:3306/securemal";
    public static final String DB_USER = "root";
    public static final String DB_PASS = "root";

    // File System Paths
    public static final String UPLOADS_DIR = "uploads";
    public static final String REPORTS_DIR = "reports";

    // Application Command Configs
    public static final String PYTHON_CMD = "python";
    public static final String ANALYZER_SCRIPT = "analysis_engine/static_analyzer.py";

    // UI Colors
    public static final Color COLOR_BG_DARK = Color.decode("#1a1a2e");
    public static final Color COLOR_ACCENT = Color.decode("#0f3460");
    public static final Color COLOR_HIGHLIGHT = Color.decode("#e94560");
    public static final Color COLOR_TEXT_WHITE = Color.decode("#ffffff");
    
    // Risk Level Colors
    public static final Color COLOR_RISK_HIGH = Color.decode("#2d0000"); // Red Tint
    public static final Color COLOR_RISK_MEDIUM = Color.decode("#2d2200"); // Orange Tint
    public static final Color COLOR_RISK_LOW = Color.decode("#002200"); // Green Tint

    static {
        // Automatically create directories if they do not exist
        File uploadsDir = new File(UPLOADS_DIR);
        if (!uploadsDir.exists()) {
            uploadsDir.mkdirs();
        }

        File reportsDir = new File(REPORTS_DIR);
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }
    }
}
