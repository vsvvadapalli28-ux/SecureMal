package com.securemal.ui.components;

public final class Icons {
    private Icons() {}

    // Severity indicators — use colored HTML spans instead of emoji
    public static final String HIGH_ICON    = "[HIGH]";
    public static final String MEDIUM_ICON  = "[MED]";
    public static final String LOW_ICON     = "[LOW]";
    public static final String PENDING_ICON = "[...]";
    public static final String DONE_ICON    = "[OK]";

    // Section headers — use Unicode symbols that Java fonts support
    public static final String SUMMARY_ICON  = "\u2139";  // ℹ
    public static final String TIMELINE_ICON = "\u23F1";  // ⏱
    public static final String FILE_ICON     = "\u25A0";  // ■
    public static final String BACK_ICON     = "\u2190";  // ←
    public static final String PDF_ICON      = "\u25B6";  // ▶
    public static final String UPLOAD_ICON   = "\u2191";  // ↑
    public static final String DELETE_ICON   = "\u2715";  // ✕
    public static final String ANALYSE_ICON  = "\u25B6";  // ▶

    /**
     * Returns an HTML-styled severity badge string for use in JLabel HTML.
     * Color is embedded directly so the badge renders correctly.
     */
    public static String severityBadge(String severity) {
        switch (severity.toLowerCase()) {
            case "high":
            case "critical":
                return "<span style='background:#8b0000; color:white; "
                     + "padding:2px 6px; border-radius:4px;'> HIGH </span>";
            case "medium":
                return "<span style='background:#8b5000; color:white; "
                     + "padding:2px 6px; border-radius:4px;'> MED </span>";
            default:
                return "<span style='background:#005000; color:white; "
                     + "padding:2px 6px; border-radius:4px;'> LOW </span>";
        }
    }

    /**
     * Returns the icon character for a given severity string.
     */
    public static String iconFor(String severity) {
        switch (severity.toLowerCase()) {
            case "high":
            case "critical": return HIGH_ICON;
            default:         return LOW_ICON;
        }
    }
}