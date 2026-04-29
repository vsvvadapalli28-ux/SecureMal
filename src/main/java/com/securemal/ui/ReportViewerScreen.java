package com.securemal.ui;

import com.securemal.config.Config;
import com.securemal.models.AnalysisReport;
import com.securemal.services.ReportService;
import com.securemal.utils.JsonUtil;
import com.securemal.ui.components.Icons;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
                } catch (ExecutionException e) {
                    // Print the REAL cause to the console so we can see it
                    System.err.println("=== ReportViewerScreen load error ===");
                    e.getCause().printStackTrace();
                    showError("Failed to load report: " + e.getCause().getMessage());
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

    private void buildUI(AnalysisReport report) {
        removeAll();
        setLayout(new BorderLayout());
        setBackground(Config.COLOR_BG_DARK);

        System.out.println("DEBUG: buildUI called with report: " + report.getFileType());
        mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setBackground(Config.COLOR_BG_DARK);
        mainContainer.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        buildHeader(report);
        System.out.println("DEBUG: after buildHeader, count: " + mainContainer.getComponentCount());

        // Add notice if dynamic analysis is missing
        if (report.getAnalysisType() == null || !report.getAnalysisType().equals("Dynamic Analysis")) {
            JPanel noticePanel = new JPanel();
            noticePanel.setBackground(new Color(255, 193, 7)); // yellow
            noticePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            JLabel noticeLabel = new JLabel("Dynamic analysis unavailable — VirtualBox VM not configured. Showing static analysis results only.");
            noticeLabel.setForeground(Color.BLACK);
            noticeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            noticePanel.add(noticeLabel);
            mainContainer.add(noticePanel);
            mainContainer.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        mainContainer.add(Box.createRigidArea(new Dimension(0, 20)));
        buildRiskScore(report);
        System.out.println("DEBUG: after buildRiskScore, count: " + mainContainer.getComponentCount());
        mainContainer.add(Box.createRigidArea(new Dimension(0, 20)));
        buildSummary(report);
        System.out.println("DEBUG: after buildSummary, count: " + mainContainer.getComponentCount());
        mainContainer.add(Box.createRigidArea(new Dimension(0, 20)));
        buildTimeline(report);
        System.out.println("DEBUG: after buildTimeline, count: " + mainContainer.getComponentCount());
        mainContainer.add(Box.createRigidArea(new Dimension(0, 20)));
        buildTechDetails(report);
        System.out.println("DEBUG: after buildTechDetails, count: " + mainContainer.getComponentCount());

        scrollPane = new JScrollPane(mainContainer);
        scrollPane.setViewportView(mainContainer);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(Config.COLOR_BG_DARK);

        System.out.println("DEBUG: mainContainer component count: " + mainContainer.getComponentCount());
        System.out.println("DEBUG: scrollPane viewport: " + scrollPane.getViewport());

        add(scrollPane, BorderLayout.CENTER);
        scrollPane.revalidate();
        scrollPane.repaint();
        revalidate();
        repaint();
    }

    private void buildHeader(AnalysisReport report) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

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

        JLabel title = new JLabel(report.getFileType(), SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.CENTER);

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
                    } catch (Exception e) {
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
        pb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        pb.setAlignmentX(Component.LEFT_ALIGNMENT);
        pb.setPreferredSize(new Dimension(200, 20));

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

        mainContainer.add(panel);
    }

    private void buildSummary(AnalysisReport report) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Config.COLOR_ACCENT);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 60, 100)),
                new EmptyBorder(15, 15, 15, 15)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("What did the analysis find?");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(title, BorderLayout.NORTH);

        String summary = report.getPlainSummary();
        if (summary == null || summary.isBlank()) {
            summary = "Analysis complete. No detailed summary available.";
        }
        summary = summary.replaceAll("\\*\\*([^\\*]+)\\*\\*", "<b>$1</b>");

        JLabel summaryLabel = new JLabel("<html><body style='width:100%; font-family:Segoe UI; "
               + "font-size:13px; color:#ffffff;'>"
               + summary + "</body></html>");
        summaryLabel.setForeground(Color.WHITE);
        summaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        summaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(summaryLabel, BorderLayout.CENTER);
        mainContainer.add(panel);
    }

    private void buildTimeline(AnalysisReport report) {
        JPanel timelinePanel = new JPanel();
        timelinePanel.setLayout(new BoxLayout(timelinePanel, BoxLayout.Y_AXIS));
        timelinePanel.setOpaque(false);
        timelinePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("What did this file do?");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setBorder(new EmptyBorder(0, 0, 15, 0));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        timelinePanel.add(title);

        List<Map<String, String>> events = JsonUtil.parseTimelineArray(report.getTimelineJson());
        timelinePanel.removeAll();
        timelinePanel.add(title);
        
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
                    case "high":
                    case "critical":
                        cardBg      = new Color(45, 0, 0);
                        borderColor = new Color(233, 69, 96);
                        break;
                    case "medium":
                        cardBg      = new Color(45, 34, 0);
                        borderColor = new Color(243, 156, 18);
                        break;
                    default:
                        cardBg      = new Color(0, 45, 0);
                        borderColor = new Color(0, 184, 148);
                        break;
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

                JLabel msgLabel = new JLabel(
                    "<html><div style='width:100%; font-family:Segoe UI; "
                    + "font-size:13px; color:#ffffff; margin-top:6px;'>"
                    + plainMsg
                    + "</div></html>"
                );
                msgLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                JLabel infoLabel = new JLabel(
                    "<html><div style='width:100%; font-family:Segoe UI; "
                    + "font-size:12px; color:#a0a0b0; font-style:italic; "
                    + "margin-top:4px;'>"
                    + "\u2139&nbsp; " + whatItMeans
                    + "</div></html>"
                );
                infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                content.add(headerLabel);
                content.add(Box.createVerticalStrut(4));
                content.add(msgLabel);
                content.add(Box.createVerticalStrut(2));
                content.add(infoLabel);

                card.add(content, BorderLayout.CENTER);

                JPanel wrapper = new JPanel(new BorderLayout());
                wrapper.setOpaque(false);
                wrapper.add(card, BorderLayout.CENTER);
                wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
                wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

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
