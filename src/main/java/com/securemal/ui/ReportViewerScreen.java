package com.securemal.ui;

import com.securemal.config.Config;
import com.securemal.models.AnalysisReport;
import com.securemal.services.ReportService;
import com.securemal.utils.JsonUtil;

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

        JButton backBtn = new JButton("← Back");
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backBtn.setBackground(Config.COLOR_BUTTON);
        backBtn.setForeground(Color.WHITE);
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

        JButton pdfBtn = new JButton("📄 Export PDF");
        pdfBtn.setBackground(Config.COLOR_BUTTON);
        pdfBtn.setForeground(Color.WHITE);
        pdfBtn.setFocusPainted(false);
        pdfBtn.setCursor(new Cursor(Cursor.HAND_CURSOR);
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

        JLabel title = new JLabel("📋 What did the analysis find?");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(title, BorderLayout.NORTH);

        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setOpaque(false);
        StyledDocument doc = textPane.getStyledDocument();

        Style defaultStyle = textPane.addStyle("Default", null);
        StyleConstants.setForeground(defaultStyle, Color.WHITE);
        StyleConstants.setFontFamily(defaultStyle, "Segoe UI");
        StyleConstants.setFontSize(defaultStyle, 13);

        Style boldStyle = textPane.addStyle("Bold", defaultStyle);
        StyleConstants.setBold(boldStyle, true);

        try {
            String plainText = report.getPlainSummary();
            int index = 0;
            while (index < plainText.length()) {
                int boldStart = plainText.indexOf("**", index);
                if (boldStart == -1) {
                    doc.insertString(doc.getLength(), plainText.substring(index), defaultStyle);
                    break;
                }
                doc.insertString(doc.getLength(), plainText.substring(index, boldStart), defaultStyle);
                int boldEnd = plainText.indexOf("**", boldStart + 2);
                if (boldEnd == -1) {
                    doc.insertString(doc.getLength(), plainText.substring(boldStart), defaultStyle);
                    break;
                }
                doc.insertString(doc.getLength(), plainText.substring(boldStart + 2, boldEnd), boldStyle);
                index = boldEnd + 2;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        panel.add(textPane, BorderLayout.CENTER);
        mainContainer.add(panel);
    }

    private void buildTimeline(AnalysisReport report) {
        JPanel timelinePanel = new JPanel();
        timelinePanel.setLayout(new BoxLayout(timelinePanel, BoxLayout.Y_AXIS));
        timelinePanel.setOpaque(false);
        timelinePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("🕐 What did this file do?");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setBorder(new EmptyBorder(0, 0, 15, 0));
        timelinePanel.add(title);

        List<Map<String, String>> events = JsonUtil.parseTimelineArray(report.getTimelineJson());
        for (Map<String, String> ev : events) {
            String severity = ev.getOrDefault("severity", "low").toLowerCase();
            Color bgColor = Config.COLOR_RISK_LOW;
            Color borderColor = Color.decode("#27ae60");
            
            if ("high".equals(severity)) {
                bgColor = Config.COLOR_RISK_HIGH;
                borderColor = Color.decode("#e94560");
            } else if ("medium".equals(severity)) {
                bgColor = Config.COLOR_RISK_MEDIUM;
                borderColor = Color.decode("#f5a623");
            }

            final Color fBgColor = bgColor;
            final Color fBorderColor = borderColor;

            JPanel card = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Main background
                    g2.setColor(fBgColor);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    
                    // Left border line
                    g2.setClip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                    g2.setColor(fBorderColor);
                    g2.fillRect(0, 0, 4, getHeight());
                    
                    g2.dispose();
                }
            };
            card.setOpaque(false);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(new EmptyBorder(14, 14, 14, 14));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

            // Row 1
            JLabel row1 = new JLabel(ev.getOrDefault("icon", "🟢") + " " + ev.getOrDefault("timestamp", "unknown"));
            row1.setForeground(Color.WHITE);
            row1.setFont(new Font("Segoe UI", Font.BOLD, 13));
            
            // Row 2
            JTextArea row2 = new JTextArea(ev.getOrDefault("plain_message", ""));
            row2.setOpaque(false);
            row2.setEditable(false);
            row2.setLineWrap(true);
            row2.setWrapStyleWord(true);
            row2.setForeground(Color.WHITE);
            row2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            row2.setBorder(new EmptyBorder(6, 0, 0, 0));

            // Row 3
            JTextArea row3 = new JTextArea("ℹ️  " + ev.getOrDefault("what_this_means", ""));
            row3.setOpaque(false);
            row3.setEditable(false);
            row3.setLineWrap(true);
            row3.setWrapStyleWord(true);
            row3.setForeground(Color.decode("#aaaaaa"));
            row3.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            row3.setBorder(new EmptyBorder(4, 0, 0, 0));

            card.add(row1);
            card.add(row2);
            card.add(row3);

            timelinePanel.add(card);
            timelinePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        mainContainer.add(timelinePanel);
    }

    private void buildTechDetails(AnalysisReport report) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JToggleButton toggleBtn = new JToggleButton("▶ Show Technical Details");
        toggleBtn.setBackground(Config.COLOR_BUTTON);
        toggleBtn.setForeground(Color.WHITE);
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
            toggleBtn.setText(selected ? "▼ Hide Technical Details" : "▶ Show Technical Details");
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
