package com.securemal.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public final class AppConfig {
    private static final String CONFIG_FILENAME = "securemal.properties";
    private static final String VBOX_PATH_PROPERTY = "virtualbox.path";
    private static final Properties properties = new Properties();
    private static final File CONFIG_FILE = new File(System.getProperty("user.dir"), CONFIG_FILENAME);

    static {
        load();
    }

    private AppConfig() {
        // Utility class
    }

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

    public static boolean isVirtualBoxPathValid() {
        String path = getVirtualBoxPath();
        return path != null && !path.isBlank() && new File(path).isFile();
    }

    public static void setVirtualBoxPath(String path) {
        properties.setProperty(VBOX_PATH_PROPERTY, path == null ? "" : path.trim());
        save();
    }
}
