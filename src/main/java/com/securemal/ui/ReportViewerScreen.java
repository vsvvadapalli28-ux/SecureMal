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

public class ReportViewerScreen extends JPanel {
    private MainFrame mainFrame;
    private int fileId;
    private ReportService reportService;

    private JPanel mainContainer;
    private JScrollPane scrollPane;

    public ReportViewerScreen(MainFrame mainFrame, int fileId) {
        this.mainFrame = mainFrame;
        this.fileId = fileId;
        this.reportService = new ReportService();

        setLayout(new BorderLayout());
        setBackground(Config.COLOR_BG_DARK);

        JLabel loadingLabel = new JLabel("Loading Report...", SwingConstants.CENTER);
        loadingLabel.setForeground(Config.COLOR_TEXT_WHITE);
        loadingLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        add(loadingLabel, BorderLayout.CENTER);

        loadReportAsync();
    }

    private void loadReportAsync() {
        SwingWorker<AnalysisReport, Void> worker = new SwingWorker<AnalysisReport, Void>() {
            @Override
            protected AnalysisReport doInBackground() throws Exception {
                return reportService.getReport(fileId);
            }

            @Override
            protected void done() {
                try {
                    AnalysisReport report = get();
                    removeAll();
                    if (report != null) {
                        buildUI(report);
                    } else {
                        JLabel error = new JLabel("Failed to load report.", SwingConstants.CENTER);
                        error.setForeground(Color.RED);
                        add(error, BorderLayout.CENTER);
                    }
                    revalidate();
                    repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void buildUI(AnalysisReport report) {
        mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setBackground(Config.COLOR_BG_DARK);
        mainContainer.setBorder(new EmptyBorder(20, 20, 20, 20));

        buildHeader(report);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 20)));
        buildRiskScore(report);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 20)));
        buildSummary(report);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 20)));
        buildTimeline(report);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 20)));
        buildTechDetails(report);

        scrollPane = new JScrollPane(mainContainer);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);
    }

    private void buildHeader(AnalysisReport report) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

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

        JLabel label = new JLabel("Risk Score: " + report.getRiskScore() + " / 100");
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel riskLabel = new JLabel(report.getRiskLabel());
        riskLabel.setForeground(report.getRiskBarColor());
        riskLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

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

        JLabel title = new JLabel(Icons.SUMMARY_ICON + " What did the analysis find?");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(title, BorderLayout.NORTH);

        String summary = report.getPlainSummary();
        if (summary == null || summary.isBlank()) {
            summary = "Analysis complete. No detailed summary available.";
        }
        // Convert word to HTML bold
        summary = summary.replaceAll("\\*\\*([^\\*]+)\\*\\*", "<b>$1</b>");
        JLabel summaryLabel = new JLabel("<html><body style='width:480px; "
               + "font-family:Segoe UI; font-size:13px; color:#ffffff;'>"
               + summary + "</body></html>");
        summaryLabel.setForeground(Color.WHITE);
        summaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        panel.add(summaryLabel, BorderLayout.CENTER);
        mainContainer.add(panel);
    }

    private void buildTimeline(AnalysisReport report) {
        JPanel timelinePanel = new JPanel();
        timelinePanel.setLayout(new BoxLayout(timelinePanel, BoxLayout.Y_AXIS));
        timelinePanel.setOpaque(false);
        timelinePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel(Icons.TIMELINE_ICON + " What did this file do?");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setBorder(new EmptyBorder(0, 0, 15, 0));
        timelinePanel.add(title);

        List<Map<String, String>> events = JsonUtil.parseTimelineArray(report.getTimelineJson());
        // Clear any previous content
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

                // --- Determine card colors ---
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

                // --- Build the card as a JPanel with BorderLayout ---
                JPanel card = new JPanel(new BorderLayout(0, 6));
                card.setBackground(cardBg);
                card.setOpaque(true);

                // Left colored accent border via MatteBorder + EmptyBorder
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, borderColor),
                    BorderFactory.createEmptyBorder(12, 14, 12, 14)
                ));

                // --- Content panel inside card (vertical stack) ---
                JPanel content = new JPanel();
                content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
                content.setOpaque(false);
                content.setBackground(cardBg);

                // Row 1: icon + timestamp header
                JLabel headerLabel = new JLabel("<html>" + badgeText + "  " + timestamp + "</html>");
                headerLabel.setForeground(Color.WHITE);
                headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
                headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                // Row 2: plain message
                JLabel msgLabel = new JLabel(
                    "<html><div style='width:530px; font-family:Segoe UI; "
                    + "font-size:13px; color:#ffffff; margin-top:6px;'>"
                    + plainMsg
                    + "</div></html>"
                );
                msgLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                // Row 3: what this means
                JLabel infoLabel = new JLabel(
                    "<html><div style='width:530px; font-family:Segoe UI; "
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

                // --- Wrapper to enforce full width and left alignment ---
                JPanel wrapper = new JPanel(new BorderLayout());
                wrapper.setOpaque(false);
                wrapper.add(card, BorderLayout.CENTER);
                wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

                timelinePanel.add(wrapper);
                timelinePanel.add(Box.createVerticalStrut(10));
            }
        }
        
        // CRITICAL: always call these two after modifying a visible panel
        timelinePanel.revalidate();
        timelinePanel.repaint();
        
        // Scroll to top after loading
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
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel label = new JLabel(labelText);
        label.setForeground(Color.LIGHT_GRAY);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setPreferredSize(new Dimension(120, 20));

        JTextField valField = new JTextField(valText != null ? valText : "N/A");
        valField.setEditable(false);
        valField.setBackground(new Color(20, 30, 50));
        valField.setForeground(Color.WHITE);
        valField.setFont(new Font("Monospaced", Font.PLAIN, 11));
        valField.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        row.add(label, BorderLayout.WEST);
        row.add(valField, BorderLayout.CENTER);
        return row;
    }
}
