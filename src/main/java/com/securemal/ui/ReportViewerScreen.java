package com.securemal.ui;

import com.securemal.config.AppConfig;
import com.securemal.config.Config;
import com.securemal.models.AnalysisReport;
import com.securemal.services.ReportService;
import com.securemal.utils.JsonUtil;
import com.securemal.ui.components.Icons;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;

public class ReportViewerScreen extends JPanel {
    private final MainFrame mainFrame;
    private final int fileId;
    private final ReportService reportService;

    private JPanel mainContainer;
    private JScrollPane scrollPane;

    public ReportViewerScreen(MainFrame mainFrame, int fileId) {
        this.mainFrame = mainFrame;
        this.fileId = fileId;
        this.reportService = new ReportService();

        setLayout(new BorderLayout());
        setBackground(Config.COLOR_BG_DARK);

        showLoading();
        loadReportAsync();
    }

    private void showLoading() {
        removeAll();
        setLayout(new BorderLayout());
        setBackground(new Color(13, 13, 26));

        JPanel loadingPanel = new JPanel(new GridBagLayout());
        loadingPanel.setBackground(new Color(13, 13, 26));

        JLabel loadingLabel = new JLabel("Loading report...");
        loadingLabel.setForeground(new Color(160, 160, 176));
        loadingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        JProgressBar spinner = new JProgressBar();
        spinner.setIndeterminate(true);
        spinner.setPreferredSize(new Dimension(200, 6));
        spinner.setBackground(new Color(42, 42, 74));
        spinner.setForeground(new Color(15, 52, 96));
        spinner.setBorderPainted(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 16, 0);
        loadingPanel.add(loadingLabel, gbc);
        gbc.gridy = 1;
        loadingPanel.add(spinner, gbc);

        add(loadingPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void loadReportAsync() {
        showLoading();
        final Timer timeoutTimer = new Timer(10000, null);

        SwingWorker<AnalysisReport, Void> worker = new SwingWorker<AnalysisReport, Void>() {
            @Override
            protected AnalysisReport doInBackground() throws Exception {
                return reportService.getReport(fileId);
            }

            @Override
            protected void done() {
                timeoutTimer.stop();
                try {
                    AnalysisReport report = get();

                    if (report == null) {
                        showError("No report found for this file. "
                            + "Please run the analysis again.");
                        return;
                    }

                    // render everything
                    buildUI(report);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showError("Report loading was interrupted.");
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable cause = e.getCause();
                    System.err.println("=== ReportViewerScreen load error ===");
                    if (cause != null) {
                        System.err.println(cause.getClass().getName() + ": " + cause.getMessage());
                    }
                    showError("Failed to load report: " + (cause != null ? cause.getMessage() : e.getMessage()));
                }
            }
        };

        timeoutTimer.addActionListener(e -> {
            if (!worker.isDone()) {
                worker.cancel(true);
                showError("Report loading timed out after 10 seconds.");
            }
        });
        timeoutTimer.setRepeats(false);
        timeoutTimer.start();

        worker.execute();
    }

    private void showError(String message) {
        removeAll();
        setLayout(new BorderLayout());
        setBackground(new Color(13, 13, 26));

        JPanel errorPanel = new JPanel(new GridBagLayout());
        errorPanel.setBackground(new Color(13, 13, 26));

        JLabel icon = new JLabel("\u26A0");  // ⚠
        icon.setFont(new Font("Segoe UI", Font.PLAIN, 48));
        icon.setForeground(new Color(243, 156, 18));

        JLabel msg = new JLabel(
            "<html><div style='text-align:center; width:400px;'>"
            + message + "</div></html>"
        );
        msg.setForeground(new Color(160, 160, 176));
        msg.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JButton backBtn = new JButton("\u2190 Back to Dashboard");
        backBtn.setBackground(new Color(15, 52, 96));
        backBtn.setForeground(Color.WHITE);
        backBtn.setFocusPainted(false);
        backBtn.setBorderPainted(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> mainFrame.showDashboard());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 16, 0);
        errorPanel.add(icon, gbc);
        gbc.gridy = 1;
        errorPanel.add(msg, gbc);
        gbc.gridy = 2;
        gbc.insets = new Insets(24, 0, 0, 0);
        errorPanel.add(backBtn, gbc);

        add(errorPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void clearMainContainer() {
        if (mainContainer != null) {
            mainContainer.removeAll();
            mainContainer.revalidate();
            mainContainer.repaint();
        }
    }

    private void buildUI(AnalysisReport report) {
        removeAll();
        setLayout(new BorderLayout());
        setBackground(new Color(13, 13, 26));
        setOpaque(true);

        // report loaded — proceed to build sections
        
        if (mainContainer == null) {
            mainContainer = new JPanel() {
                @Override
                public Dimension getPreferredSize() {
                    int parentWidth = (getParent() != null && getParent().getWidth() > 0)
                        ? getParent().getWidth() : 900;
                    Dimension natural = super.getPreferredSize();
                    return new Dimension(
                        Math.max(parentWidth, natural.width),
                        natural.height
                    );
                }
            };
            mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
            mainContainer.setBackground(new Color(13, 13, 26));
            mainContainer.setOpaque(true);
            mainContainer.setBorder(BorderFactory.createEmptyBorder(20, 15, 40, 15));
            mainContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
            mainContainer.setMinimumSize(new Dimension(0, 0));
            mainContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        } else {
            mainContainer.removeAll();
            mainContainer.revalidate();
            mainContainer.repaint();
        }

        buildHeader(report);


        // Inform user about analysis mode (static-only vs dynamic)
        if (report.getAnalysisType() == null || !report.getAnalysisType().toLowerCase().contains("dynamic")) {
            boolean vboxFound = AppConfig.isVirtualBoxPathValid();
            Color noticeBg = vboxFound ? new Color(20, 50, 90) : new Color(80, 40, 0);
            Color noticeBorder = vboxFound ? new Color(70, 130, 200) : new Color(243, 156, 18);

            JPanel noticePanel = new JPanel(new BorderLayout(10, 0));
            noticePanel.setBackground(noticeBg);
            noticePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, noticeBorder),
                new EmptyBorder(10, 12, 10, 12)
            ));
            noticePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            noticePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

            String noticeText;
            if (vboxFound) {
                noticeText = "<html><body style='font-family:Segoe UI; font-size:12px; color:#c8daf5;'>"
                    + "<b>Static analysis results.</b> VirtualBox is installed — use the ‘Dynamic Analysis’ button on the dashboard to add real-time behavioural data."
                    + "</body></html>";
            } else {
                noticeText = "<html><body style='font-family:Segoe UI; font-size:12px; color:#ffd080;'>"
                    + "<b>Static analysis results only.</b> VirtualBox is not installed "
                    + "or VM '" + AppConfig.getVmName() + "' is not configured. "
                    + "Dynamic analysis is unavailable."
                    + "</body></html>";
            }

            JLabel noticeLabel = new JLabel(noticeText);
            noticeLabel.setOpaque(false);
            noticePanel.add(noticeLabel, BorderLayout.CENTER);

            if (!vboxFound) {
                JButton setupBtn = new JButton("Setup Guide");
                setupBtn.setBackground(new Color(243, 156, 18));
                setupBtn.setForeground(Color.BLACK);
                setupBtn.setOpaque(true);
                setupBtn.setBorderPainted(false);
                setupBtn.setFocusPainted(false);
                setupBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
                setupBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                setupBtn.addActionListener(e ->
                    VirtualBoxSetupDialog.show(
                        (java.awt.Frame) SwingUtilities.getWindowAncestor(this)));
                noticePanel.add(setupBtn, BorderLayout.EAST);
            }

            mainContainer.add(noticePanel);
            mainContainer.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        mainContainer.add(Box.createRigidArea(new Dimension(0, 20)));
        buildRiskScore(report);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 20)));
        buildSummary(report);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 20)));
        buildTimeline(report);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 20)));
        buildDynamicSection(report);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 20)));
        buildTechDetails(report);

        // FIX C: Create scrollPane and set viewport LAST (after all content added)
        scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(new Color(13, 13, 26));
        scrollPane.getViewport().setBackground(new Color(13, 13, 26));
        scrollPane.getViewport().setOpaque(true);
        
        // CRITICAL: Set viewport LAST, after all content is in mainContainer
        scrollPane.setViewportView(mainContainer);
        scrollPane.getViewport().setViewPosition(new Point(0, 0));

        // FIX A: Add scrollPane using BorderLayout.CENTER so it fills available space
        add(scrollPane, BorderLayout.CENTER);
        
        // FIX F: Add component resize listener
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                mainContainer.revalidate();
                scrollPane.revalidate();
            }

            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    mainContainer.revalidate();
                    mainContainer.repaint();
                    scrollPane.revalidate();
                    scrollPane.repaint();
                    scrollPane.getVerticalScrollBar().setValue(0);
                });
            }
        });
        
        revalidate();
        repaint();
    }

    private void buildHeader(AnalysisReport report) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        // FIX E: Component alignment for BoxLayout
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton backBtn = new JButton(Icons.BACK_ICON + " Back");
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backBtn.setBackground(Config.COLOR_BUTTON);
        backBtn.setForeground(Color.WHITE);
        backBtn.setOpaque(true);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> {
            mainFrame.showDashboard();
            // We assume dashboard refresh happens on ComponentShown/setVisible
        });
        header.add(backBtn, BorderLayout.WEST);

        JLabel title = new JLabel("<html><body style='width:260px;'>"
            + report.getFileType() + "</body></html>");
        title.setToolTipText(report.getFileType());
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        title.setHorizontalAlignment(SwingConstants.LEFT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.add(title, BorderLayout.WEST);
        header.add(titlePanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        JLabel badge = new JLabel(report.getRiskLabel().toUpperCase() + " RISK") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(report.getRiskBarColor());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        badge.setForeground(Color.WHITE);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 14));
        badge.setBorder(new EmptyBorder(5, 12, 5, 12));
        badge.setOpaque(false);

        JButton pdfBtn = new JButton("Export PDF");
        pdfBtn.setBackground(Config.COLOR_BUTTON);
        pdfBtn.setForeground(Color.WHITE);
        pdfBtn.setOpaque(true);
        pdfBtn.setBorderPainted(false);
        pdfBtn.setFocusPainted(false);
        pdfBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        pdfBtn.addActionListener(e -> exportPdf(report));

        rightPanel.add(badge);
        rightPanel.add(pdfBtn);
        header.add(rightPanel, BorderLayout.EAST);

        // FIX E: Set max size AFTER header is fully constructed
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, header.getPreferredSize().height + 10));
        mainContainer.add(header);
    }

    private void exportPdf(AnalysisReport report) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Documents", "pdf"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".pdf")) {
                f = new File(f.getAbsolutePath() + ".pdf");
            }
            String path = f.getAbsolutePath();
            
            JDialog progressDialog = new JDialog(mainFrame, "Exporting", true);
            JProgressBar pb = new JProgressBar();
            pb.setIndeterminate(true);
            progressDialog.add(new JLabel("Exporting PDF, please wait..."), BorderLayout.NORTH);
            progressDialog.add(pb, BorderLayout.CENTER);
            progressDialog.setSize(250, 80);
            progressDialog.setLocationRelativeTo(this);

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    reportService.exportReportAsPDF(report.getId(), path);
                    return null;
                }
                @Override
                protected void done() {
                    progressDialog.dispose();
                    try {
                        get();
                        JOptionPane.showMessageDialog(ReportViewerScreen.this, "PDF saved to:\n" + path, "Success", JOptionPane.INFORMATION_MESSAGE);
                    } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                        JOptionPane.showMessageDialog(ReportViewerScreen.this, "Failed to export PDF: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
            progressDialog.setVisible(true);
        }
    }

    private void buildRiskScore(AnalysisReport report) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JProgressBar pb = new JProgressBar(0, 100);
        pb.setValue(report.getRiskScore());
        pb.setForeground(report.getRiskBarColor());
        pb.setBackground(Config.COLOR_ACCENT);
        pb.setMaximumSize(new Dimension(600, 20));
        pb.setPreferredSize(new Dimension(600, 20));
        pb.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel("Risk Score: " + report.getRiskScore() + " / 100");
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));

        JLabel riskLabel = new JLabel(report.getRiskLabel());
        riskLabel.setForeground(report.getRiskBarColor());
        riskLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        riskLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        riskLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));

        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(pb);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(riskLabel);

        // FIX E: Set max size for fixed-height risk panel
        panel.setMaximumSize(new Dimension(
            Integer.MAX_VALUE, 
            panel.getPreferredSize().height + 10));
        
        mainContainer.add(panel);
    }

    private void buildSummary(AnalysisReport report) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Config.COLOR_ACCENT);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 60, 100)),
                new EmptyBorder(15, 15, 15, 15)
        ));
        // FIX E: Component alignment (summary grows naturally, no max size)
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel summaryBody = new JPanel();
        summaryBody.setLayout(new BoxLayout(summaryBody, BoxLayout.Y_AXIS));
        summaryBody.setOpaque(false);
        summaryBody.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("What did the analysis find?");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        summaryBody.add(title);

        String summary = report.getPlainSummary();
        if (summary == null || summary.isBlank()) {
            summary = "Analysis complete. No detailed summary available.";
        }
        summary = summary.replaceAll("\\*\\*([^\\*]+)\\*\\*", "$1");

        JTextArea summaryArea = new JTextArea(summary);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setEditable(false);
        summaryArea.setOpaque(false);
        summaryArea.setFocusable(false);
        summaryArea.setForeground(Color.WHITE);
        summaryArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        summaryArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        summaryArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        summaryArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        summaryArea.setPreferredSize(new Dimension(900, summaryArea.getPreferredSize().height));

        summaryBody.add(summaryArea);
        panel.add(summaryBody, BorderLayout.CENTER);
        mainContainer.add(panel);
    }

    /**
     * Renders the Dynamic Analysis section when the report was produced by a full
     * dynamic run (analysis_type contains "dynamic"). Shows a distinct blue header
     * and lists any dynamic timeline events separately from the static timeline.
     * When the report is static-only, this method renders an explanatory card
     * rather than an empty section.
     */
    private void buildDynamicSection(AnalysisReport report) {
        boolean isDynamic = report.getAnalysisType() != null
                && report.getAnalysisType().toLowerCase().contains("dynamic");

        JPanel sectionPanel = new JPanel();
        sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.Y_AXIS));
        sectionPanel.setOpaque(false);
        sectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Section heading
        JLabel heading = new JLabel(isDynamic ? "🟢  Dynamic Analysis Results" : "⚪  Dynamic Analysis");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 16));
        heading.setForeground(isDynamic ? new Color(0, 200, 150) : new Color(120, 120, 140));
        heading.setBorder(new EmptyBorder(0, 0, 12, 0));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        sectionPanel.add(heading);

        if (!isDynamic) {
            // Show a "not run" explanation card
            JPanel card = new JPanel(new BorderLayout(10, 0));
            card.setBackground(new Color(20, 25, 45));
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, new Color(70, 80, 120)),
                new EmptyBorder(14, 14, 14, 14)
            ));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));

            boolean vboxFound = AppConfig.isVirtualBoxPathValid();
            String explanation = vboxFound
                ? "<html><body style='font-family:Segoe UI; font-size:12px; color:#a0a0b0;'>"
                    + "Dynamic analysis was not run for this file. "
                    + "<b style='color:#e0e0e0;'>VirtualBox is installed</b> — use the "
                    + "<i>Dynamic Analysis</i> button on the dashboard to run the file inside VM "
                    + "<b style='color:#c8daf5;'>" + AppConfig.getVmName() + "</b> "
                    + "and capture real-time behaviour.<br><br>"
                    + "<span style='color:#707080;'>Dynamic results will appear here after a successful run.</span>"
                    + "</body></html>"
                : "<html><body style='font-family:Segoe UI; font-size:12px; color:#a0a0b0;'>"
                    + "Dynamic analysis requires VirtualBox to be installed and a clean VM snapshot to be configured. "
                    + "Click <b style='color:#ffd080;'>Setup Guide</b> to get started."
                    + "</body></html>";

            JLabel expLabel = new JLabel(explanation);
            card.add(expLabel, BorderLayout.CENTER);

            if (!vboxFound) {
                JButton setupBtn = new JButton("Setup Guide");
                setupBtn.setBackground(new Color(243, 156, 18));
                setupBtn.setForeground(Color.BLACK);
                setupBtn.setOpaque(true);
                setupBtn.setBorderPainted(false);
                setupBtn.setFocusPainted(false);
                setupBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
                setupBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                setupBtn.addActionListener(e ->
                    VirtualBoxSetupDialog.show(
                        (java.awt.Frame) SwingUtilities.getWindowAncestor(this)));
                card.add(setupBtn, BorderLayout.EAST);
            }

            sectionPanel.add(card);
        } else {
            // Parse and display dynamic-only events (those with a real timestamp, not "static analysis")
            List<Map<String, String>> allEvents = JsonUtil.parseTimelineArray(report.getTimelineJson());
            if (allEvents == null) allEvents = new java.util.ArrayList<>();

            java.util.List<Map<String, String>> dynEvents = new java.util.ArrayList<>();
            for (Map<String, String> ev : allEvents) {
                String ts = ev.getOrDefault("timestamp", "static analysis");
                if (!"static analysis".equalsIgnoreCase(ts.trim())) {
                    dynEvents.add(ev);
                }
            }

            if (dynEvents.isEmpty()) {
                JLabel noEvents = new JLabel(
                    "<html><body style='color:#a0a0b0; font-style:italic;'>"
                    + "Dynamic analysis completed but no behavioural events were captured."
                    + "</body></html>"
                );
                noEvents.setAlignmentX(Component.LEFT_ALIGNMENT);
                noEvents.setBorder(new EmptyBorder(10, 10, 10, 10));
                sectionPanel.add(noEvents);
            } else {
                JLabel countLabel = new JLabel(
                    "<html><body style='font-family:Segoe UI; font-size:11px; color:#a0d0b0;'>"
                    + dynEvents.size() + " behavioural event(s) captured at runtime"
                    + "</body></html>"
                );
                countLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                countLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
                sectionPanel.add(countLabel);

                for (Map<String, String> event : dynEvents) {
                    String severity  = event.getOrDefault("severity", "low");
                    String timestamp = event.getOrDefault("timestamp", "");
                    String plainMsg  = event.getOrDefault("plain_message", "");
                    String whatMeans = event.getOrDefault("what_this_means", "");

                    Color cardBg     = new Color(10, 35, 55);
                    Color borderCol  = new Color(0, 130, 200);
                    switch (severity.toLowerCase()) {
                        case "high", "critical" -> {
                            cardBg    = new Color(45, 0, 0);
                            borderCol = new Color(233, 69, 96);
                        }
                        case "medium" -> {
                            cardBg    = new Color(45, 34, 0);
                            borderCol = new Color(243, 156, 18);
                        }
                        default -> {}
                    }

                    JPanel card = new JPanel(new BorderLayout(0, 6));
                    card.setBackground(cardBg);
                    card.setOpaque(true);
                    card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 4, 0, 0, borderCol),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)
                    ));
                    card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

                    JPanel content = new JPanel();
                    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
                    content.setOpaque(false);

                    JLabel timeLabel = new JLabel(
                        "<html>" + com.securemal.ui.components.Icons.severityBadge(severity)
                        + "  \u23F1 " + timestamp + "</html>"
                    );
                    timeLabel.setForeground(Color.WHITE);
                    timeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    timeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JTextArea msgArea = new JTextArea(plainMsg);
                    msgArea.setLineWrap(true);
                    msgArea.setWrapStyleWord(true);
                    msgArea.setEditable(false);
                    msgArea.setOpaque(false);
                    msgArea.setForeground(Color.WHITE);
                    msgArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    msgArea.setBorder(BorderFactory.createEmptyBorder());
                    msgArea.setAlignmentX(Component.LEFT_ALIGNMENT);
                    msgArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

                    content.add(timeLabel);
                    content.add(Box.createVerticalStrut(4));
                    content.add(msgArea);

                    if (!whatMeans.isBlank() && !whatMeans.equals(plainMsg)) {
                        JTextArea infoArea = new JTextArea("\u2139 " + whatMeans);
                        infoArea.setLineWrap(true);
                        infoArea.setWrapStyleWord(true);
                        infoArea.setEditable(false);
                        infoArea.setOpaque(false);
                        infoArea.setForeground(new Color(160, 160, 176));
                        infoArea.setFont(new Font("Segoe UI", Font.ITALIC, 11));
                        infoArea.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
                        infoArea.setAlignmentX(Component.LEFT_ALIGNMENT);
                        infoArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
                        content.add(infoArea);
                    }

                    card.add(content, BorderLayout.CENTER);

                    JPanel wrapper = new JPanel(new BorderLayout());
                    wrapper.setOpaque(false);
                    wrapper.add(card, BorderLayout.CENTER);
                    wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
                    wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
                    wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
                    sectionPanel.add(wrapper);
                }
            }
        }

        mainContainer.add(sectionPanel);
    }

    private void buildTimeline(AnalysisReport report) {
        JPanel timelinePanel = new JPanel();
        timelinePanel.setLayout(new BoxLayout(timelinePanel, BoxLayout.Y_AXIS));
        timelinePanel.setOpaque(false);
        // FIX E: Component alignment (timeline grows naturally, no max size)
        timelinePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("What did this file do?");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setBorder(new EmptyBorder(0, 0, 15, 0));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        timelinePanel.add(title);

        List<Map<String, String>> events = JsonUtil.parseTimelineArray(report.getTimelineJson());
        
        if (events == null || events.isEmpty()) {
            JLabel empty = new JLabel(
                "<html><body style='color:#a0a0b0; font-style:italic;'>"
                + "No behavioral events were detected during analysis."
                + "</body></html>"
            );
            empty.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            timelinePanel.add(empty);
        } else {
            for (Map<String, String> event : events) {
                String severity    = event.getOrDefault("severity", "low");
                String timestamp   = event.getOrDefault("timestamp", "static analysis");
                String plainMsg    = event.getOrDefault("plain_message", "");
                String whatItMeans = event.getOrDefault("what_this_means", "");
                String badgeText   = Icons.severityBadge(severity);

                Color cardBg;
                Color borderColor;
                switch (severity.toLowerCase()) {
                    case "high", "critical" -> {
                        cardBg = new Color(45, 0, 0);
                        borderColor = new Color(233, 69, 96);
                    }
                    case "medium" -> {
                        cardBg = new Color(45, 34, 0);
                        borderColor = new Color(243, 156, 18);
                    }
                    default -> {
                        cardBg = new Color(0, 45, 0);
                        borderColor = new Color(0, 184, 148);
                    }
                }

                JPanel card = new JPanel(new BorderLayout(0, 6));
                card.setBackground(cardBg);
                card.setOpaque(true);
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, borderColor),
                    BorderFactory.createEmptyBorder(12, 14, 12, 14)
                ));
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

                JPanel content = new JPanel();
                content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
                content.setOpaque(false);
                content.setBackground(cardBg);
                content.setAlignmentX(Component.LEFT_ALIGNMENT);

                JLabel headerLabel = new JLabel("<html>" + badgeText + "  " + timestamp + "</html>");
                headerLabel.setForeground(Color.WHITE);
                headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
                headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                JTextArea msgArea = new JTextArea(plainMsg);
                msgArea.setLineWrap(true);
                msgArea.setWrapStyleWord(true);
                msgArea.setEditable(false);
                msgArea.setOpaque(false);
                msgArea.setForeground(Color.WHITE);
                msgArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                msgArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                msgArea.setAlignmentX(Component.LEFT_ALIGNMENT);
                msgArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

                JTextArea infoArea = new JTextArea("\u2139 " + whatItMeans);
                infoArea.setLineWrap(true);
                infoArea.setWrapStyleWord(true);
                infoArea.setEditable(false);
                infoArea.setOpaque(false);
                infoArea.setForeground(new Color(160, 160, 176));
                infoArea.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                infoArea.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
                infoArea.setAlignmentX(Component.LEFT_ALIGNMENT);
                infoArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

                content.add(headerLabel);
                content.add(Box.createVerticalStrut(4));
                content.add(msgArea);
                content.add(Box.createVerticalStrut(2));
                content.add(infoArea);

                card.add(content, BorderLayout.CENTER);

                JPanel wrapper = new JPanel(new BorderLayout());
                wrapper.setOpaque(false);
                wrapper.add(card, BorderLayout.CENTER);
                wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
                wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
                wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

                timelinePanel.add(wrapper);
                timelinePanel.add(Box.createVerticalStrut(10));
            }
        }
        
        timelinePanel.revalidate();
        timelinePanel.repaint();
        
        SwingUtilities.invokeLater(() -> {
            if (scrollPane != null) {
                scrollPane.getVerticalScrollBar().setValue(0);
            }
        });

        mainContainer.add(timelinePanel);
    }

    private void buildTechDetails(AnalysisReport report) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);
        // FIX E: Component alignment (tech details grows naturally, no max size)
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JToggleButton toggleBtn = new JToggleButton("Show Technical Details");
        toggleBtn.setBackground(Config.COLOR_BUTTON);
        toggleBtn.setForeground(Color.WHITE);
        toggleBtn.setOpaque(true);
        toggleBtn.setBorderPainted(false);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel hiddenPanel = new JPanel();
        hiddenPanel.setLayout(new BoxLayout(hiddenPanel, BoxLayout.Y_AXIS));
        hiddenPanel.setBackground(Config.COLOR_BG_DARK);
        hiddenPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        hiddenPanel.setVisible(false);
        hiddenPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hiddenPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        hiddenPanel.add(createTechRow("MD5:", report.getMd5Hash()));
        hiddenPanel.add(createTechRow("SHA256:", report.getSha256Hash()));
        hiddenPanel.add(createTechRow("File Type:", report.getFileType()));
        hiddenPanel.add(createTechRow("Suspicious Strings:", report.getSuspiciousStrings()));
        hiddenPanel.add(createTechRow("PE Info:", report.getPeInfo()));
        hiddenPanel.add(createTechRow("Analysis Type:", report.getAnalysisType()));
        hiddenPanel.add(createTechRow("Timestamp:", report.getCreatedAt().toString()));

        toggleBtn.addActionListener(e -> {
            boolean selected = toggleBtn.isSelected();
            hiddenPanel.setVisible(selected);
            toggleBtn.setText(selected ? "▼ Hide Technical Details" : "Show Technical Details");
            revalidate();
            repaint();
        });

        wrapper.add(toggleBtn);
        wrapper.add(Box.createRigidArea(new Dimension(0, 5)));
        wrapper.add(hiddenPanel);

        mainContainer.add(wrapper);
    }

    private JPanel createTechRow(String labelText, String valText) {
        JPanel row = new JPanel(new BorderLayout(10, 8));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        row.setBorder(new EmptyBorder(0, 0, 8, 0));

        JLabel label = new JLabel(labelText);
        label.setForeground(Color.LIGHT_GRAY);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setPreferredSize(new Dimension(140, 20));
        label.setVerticalAlignment(SwingConstants.TOP);
        label.setMaximumSize(new Dimension(140, 20));

        JTextArea valArea = new JTextArea(valText != null ? valText : "N/A");
        valArea.setEditable(false);
        valArea.setLineWrap(true);
        valArea.setWrapStyleWord(true);
        valArea.setOpaque(true);
        valArea.setBackground(new Color(20, 30, 50));
        valArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        valArea.setFocusable(false);
        valArea.setForeground(Color.WHITE);
        valArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        valArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        valArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        row.add(label, BorderLayout.WEST);
        row.add(valArea, BorderLayout.CENTER);
        return row;
    }
}
