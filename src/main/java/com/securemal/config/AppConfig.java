package com.securemal.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public final class AppConfig {
    private static final String CONFIG_FILENAME = "securemal.properties";
    private static final String VBOX_PATH_PROPERTY = "virtualbox.path";
    private static final String VM_NAME_PROPERTY    = "vm.name";
    private static final String VM_SNAPSHOT_PROPERTY = "vm.snapshot";

    /** Default VM name used when no value is persisted. */
    public static final String DEFAULT_VM_NAME       = "SecureMal-Clean";
    /** Default snapshot name used when no value is persisted. */
    public static final String DEFAULT_SNAPSHOT_NAME = "Clean";

    private static final Properties properties = new Properties();
    private static final File CONFIG_FILE =
            new File(System.getProperty("user.dir"), CONFIG_FILENAME);

    static {
        load();
    }

    private AppConfig() {
        // Utility class — no instances
    }

    // ──────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────

    private static void load() {
        if (!CONFIG_FILE.exists()) {
            return;
        }
        try (FileInputStream stream = new FileInputStream(CONFIG_FILE)) {
            properties.load(stream);
        } catch (IOException e) {
            System.err.println("Failed to load application configuration: " + e.getMessage());
        }
    }

    private static void save() {
        try (FileOutputStream stream = new FileOutputStream(CONFIG_FILE)) {
            properties.store(stream, "SecureMal application settings");
        } catch (IOException e) {
            System.err.println("Failed to save application configuration: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────
    // VirtualBox executable path
    // ──────────────────────────────────────────────────────────────

    public static String getVirtualBoxPath() {
        String envPath = System.getenv("VBOXMANAGE");
        if (envPath != null && !envPath.isBlank()) {
            return envPath.trim();
        }
        load();
        String configured = properties.getProperty(VBOX_PATH_PROPERTY, "").trim();
        if (!configured.isBlank()) {
            return configured;
        }
        String autoDetected = findDefaultVirtualBoxPath();
        if (!autoDetected.isBlank()) {
            properties.setProperty(VBOX_PATH_PROPERTY, autoDetected);
            save();
            return autoDetected;
        }
        String pathOnPath = findExecutableInPath("VBoxManage");
        if (!pathOnPath.isBlank()) {
            properties.setProperty(VBOX_PATH_PROPERTY, pathOnPath);
            save();
            return pathOnPath;
        }
        return "";
    }

    public static boolean isVirtualBoxPathValid() {
        String path = getVirtualBoxPath();
        return path != null && !path.isBlank() && new File(path).isFile();
    }

    public static void setVirtualBoxPath(String path) {
        properties.setProperty(VBOX_PATH_PROPERTY, path == null ? "" : path.trim());
        save();
    }

    // ──────────────────────────────────────────────────────────────
    // VM name (which registered VirtualBox VM to use)
    // ──────────────────────────────────────────────────────────────

    /**
     * Returns the configured VM name, falling back to {@link #DEFAULT_VM_NAME}
     * if nothing is saved in properties.
     */
    public static String getVmName() {
        load();
        String v = properties.getProperty(VM_NAME_PROPERTY, "").trim();
        return v.isBlank() ? DEFAULT_VM_NAME : v;
    }

    /** Persists the VM name to {@code securemal.properties}. */
    public static void setVmName(String name) {
        properties.setProperty(VM_NAME_PROPERTY, name == null ? "" : name.trim());
        save();
    }

    // ──────────────────────────────────────────────────────────────
    // VM snapshot name
    // ──────────────────────────────────────────────────────────────

    /**
     * Returns the configured snapshot name, falling back to
     * {@link #DEFAULT_SNAPSHOT_NAME} if nothing is saved.
     */
    public static String getSnapshotName() {
        load();
        String v = properties.getProperty(VM_SNAPSHOT_PROPERTY, "").trim();
        return v.isBlank() ? DEFAULT_SNAPSHOT_NAME : v;
    }

    /** Persists the snapshot name to {@code securemal.properties}. */
    public static void setSnapshotName(String name) {
        properties.setProperty(VM_SNAPSHOT_PROPERTY, name == null ? "" : name.trim());
        save();
    }

    // ──────────────────────────────────────────────────────────────
    // Path-discovery helpers (unchanged)
    // ──────────────────────────────────────────────────────────────

    private static String findDefaultVirtualBoxPath() {
        String[] commonPaths = new String[] {
            "C:\\Program Files\\Oracle\\VirtualBox\\VBoxManage.exe",
            "C:\\Program Files (x86)\\Oracle\\VirtualBox\\VBoxManage.exe",
            "/usr/bin/VBoxManage",
            "/usr/local/bin/VBoxManage"
        };
        for (String path : commonPaths) {
            File file = new File(path);
            if (file.isFile()) {
                return path;
            }
        }
        return "";
    }

    private static String findExecutableInPath(String executable) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isBlank()) {
            return "";
        }
        String separator = System.getProperty("path.separator");
        String fileExt = System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "";
        for (String dir : pathEnv.split(separator)) {
            if (dir == null || dir.isBlank()) {
                continue;
            }
            File candidate = new File(dir, executable + fileExt);
            if (candidate.isFile()) {
                return candidate.getAbsolutePath();
            }
        }
        return "";
    }
}
