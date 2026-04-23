package com.securemal.ui;

import com.securemal.config.Config;
import com.securemal.controllers.AnalysisController;
import com.securemal.models.UploadedFile;
import com.securemal.models.User;
import com.securemal.services.FileManagementService;
import com.securemal.state.AppState;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

public class DashboardScreen extends JPanel {
    private MainFrame mainFrame;
    private JLabel lblWelcome;
    private JTable fileTable;
    private DefaultTableModel tableModel;
    private FileManagementService fileService;
    private List<UploadedFile> currentFiles;

    public DashboardScreen(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.fileService = new FileManagementService();
        
        setBackground(Config.COLOR_BG_DARK);
        setLayout(new BorderLayout());

        // Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Config.COLOR_BG_DARK);
        topBar.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("🔒 SecureMal");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Config.COLOR_TEXT_WHITE);
        topBar.add(title, BorderLayout.WEST);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        userPanel.setBackground(Config.COLOR_BG_DARK);
        
        lblWelcome = new JLabel("Welcome, User");
        lblWelcome.setForeground(Config.COLOR_TEXT_WHITE);
        lblWelcome.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        JButton btnLogout = new JButton("Logout");
        btnLogout.setBackground(new Color(200, 50, 50));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFocusPainted(false);
        btnLogout.setMargin(new Insets(2, 10, 2, 10));
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> {
            AppState.getInstance().logout();
            mainFrame.showLogin();
        });

        userPanel.add(lblWelcome);
        userPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        userPanel.add(btnLogout);
        topBar.add(userPanel, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // Center Table Area
        String[] columns = {"#", "File Name", "Size", "Uploaded", "Risk", "Status", "Action"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only Action column is editable (for button click)
            }
        };

        fileTable = new JTable(tableModel);
        fileTable.setBackground(Config.COLOR_ACCENT);
        fileTable.setForeground(Config.COLOR_TEXT_WHITE);
        fileTable.setRowHeight(35);
        fileTable.getTableHeader().setBackground(new Color(10, 30, 60));
        fileTable.getTableHeader().setForeground(Config.COLOR_TEXT_WHITE);
        fileTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        fileTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        fileTable.setSelectionBackground(new Color(30, 80, 150));
        fileTable.setSelectionForeground(Color.WHITE);
        
        // Custom Row Renderer for alternating colors
        fileTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Config.COLOR_ACCENT : new Color(10, 42, 80));
                }
                return c;
            }
        });

        // Custom Risk Badge Renderer
        fileTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setOpaque(true);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                String risk = value != null ? value.toString() : "Pending";
                if ("High".equalsIgnoreCase(risk)) {
                    label.setBackground(Config.COLOR_RISK_HIGH);
                    label.setForeground(Color.RED);
                } else if ("Medium".equalsIgnoreCase(risk)) {
                    label.setBackground(Config.COLOR_RISK_MEDIUM);
                    label.setForeground(Color.ORANGE);
                } else if ("Low".equalsIgnoreCase(risk)) {
                    label.setBackground(Config.COLOR_RISK_LOW);
                    label.setForeground(Color.GREEN);
                } else {
                    label.setBackground(Color.DARK_GRAY);
                    label.setForeground(Color.LIGHT_GRAY);
                }
                return label;
            }
        });

        // Action Column Button Renderer/Editor
        fileTable.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer());
        fileTable.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox()));

        // Double click listener for rows
        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = fileTable.getSelectedRow();
                    if (row != -1 && currentFiles != null && currentFiles.size() > row) {
                        UploadedFile uf = currentFiles.get(row);
                        if ("Analysed".equals(uf.getStatus())) {
                            mainFrame.showReport(uf.getId());
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(fileTable);
        scrollPane.getViewport().setBackground(Config.COLOR_BG_DARK);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomBar.setBackground(Config.COLOR_BG_DARK);
        bottomBar.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        JButton btnUpload = new JButton("📁 Upload File");
        btnUpload.setFont(new Font("SansSerif", Font.BOLD, 16));
        btnUpload.setBackground(Config.COLOR_ACCENT);
        btnUpload.setForeground(Color.WHITE);
        btnUpload.setPreferredSize(new Dimension(250, 45));
        btnUpload.setFocusPainted(false);
        btnUpload.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnUpload.addActionListener(e -> performUpload());

        bottomBar.add(btnUpload);
        add(bottomBar, BorderLayout.SOUTH);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            User currentUser = AppState.getInstance().getCurrentUser();
            if (currentUser != null) {
                lblWelcome.setText("Welcome, " + currentUser.getUsername());
            }
            refreshTable();
        }
    }

    public void refreshTable() {
        User currentUser = AppState.getInstance().getCurrentUser();
        if (currentUser == null) return;

        tableModel.setRowCount(0);
        Object[] loadingRow = {"", "Loading files...", "", "", "", "", ""};
        tableModel.addRow(loadingRow);

        SwingWorker<List<UploadedFile>, Void> worker = new SwingWorker<List<UploadedFile>, Void>() {
            @Override
            protected List<UploadedFile> doInBackground() throws Exception {
                return fileService.getFilesForUser(currentUser.getId());
            }

            @Override
            protected void done() {
                try {
                    currentFiles = get();
                    tableModel.setRowCount(0);
                    for (int i = 0; i < currentFiles.size(); i++) {
                        UploadedFile uf = currentFiles.get(i);
                        Object[] rowData = {
                                i + 1,
                                uf.getOriginalFilename(),
                                uf.getFileSizeFormatted(),
                                uf.getUploadedAt().toString(),
                                uf.getRiskBadge(),
                                uf.getStatus(),
                                "View Report"
                        };
                        tableModel.addRow(rowData);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    tableModel.setRowCount(0);
                }
            }
        };
        worker.execute();
    }

    private void performUpload() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Executables & Documents", 
                "exe", "dll", "bat", "ps1", "vbs", "js", "zip", "pdf", "docx", "xlsx");
        chooser.setFileFilter(filter);
        
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            User currentUser = AppState.getInstance().getCurrentUser();
            
            // Show progress dialog
            JDialog progressDialog = new JDialog(mainFrame, "Uploading", true);
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            progressDialog.add(new JLabel("Uploading file, please wait..."), BorderLayout.NORTH);
            progressDialog.add(progressBar, BorderLayout.CENTER);
            progressDialog.setSize(300, 100);
            progressDialog.setLocationRelativeTo(this);
            
            SwingWorker<UploadedFile, Void> worker = new SwingWorker<UploadedFile, Void>() {
                @Override
                protected UploadedFile doInBackground() throws Exception {
                    return fileService.uploadFile(selectedFile, currentUser.getId());
                }

                @Override
                protected void done() {
                    progressDialog.dispose();
                    try {
                        UploadedFile uf = get();
                        if (uf != null) {
                            refreshTable();
                            int opt = JOptionPane.showConfirmDialog(DashboardScreen.this, 
                                    "File uploaded successfully! Analyse now?", 
                                    "Upload Complete", JOptionPane.YES_NO_CANCEL_OPTION);
                            if (opt == JOptionPane.YES_OPTION) {
                                AnalysisController.runStaticAnalysis(uf.getId(), DashboardScreen.this);
                            } else if (opt == JOptionPane.NO_OPTION) {
                                int dynOpt = JOptionPane.showConfirmDialog(DashboardScreen.this,
                                        "Run Dynamic Analysis (VM Sandbox) instead?", "Dynamic Analysis", JOptionPane.YES_NO_OPTION);
                                if (dynOpt == JOptionPane.YES_OPTION) {
                                    AnalysisController.runDynamicAnalysis(uf.getId(), DashboardScreen.this);
                                }
                            }
                        } else {
                            JOptionPane.showMessageDialog(DashboardScreen.this, 
                                    "Error uploading file.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            worker.execute();
            progressDialog.setVisible(true); // Blocks until dispose
        }
    }

    // Custom Button Renderer
    class ButtonRenderer extends JPanel implements TableCellRenderer {
        private JButton btnStatic;
        private JButton btnDynamic;

        public ButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
            setOpaque(true);
            btnStatic = new JButton("View Report");
            btnStatic.setFocusPainted(false);
            btnStatic.setFont(new Font("SansSerif", Font.BOLD, 10));

            btnDynamic = new JButton("Dynamic Analysis");
            btnDynamic.setFocusPainted(false);
            btnDynamic.setFont(new Font("SansSerif", Font.BOLD, 10));

            add(btnStatic);
            add(btnDynamic);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(row % 2 == 0 ? Config.COLOR_ACCENT : new Color(10, 42, 80));
            }
            
            if (currentFiles != null && currentFiles.size() > row) {
                UploadedFile uf = currentFiles.get(row);
                if ("Analysed".equals(uf.getStatus())) {
                    btnStatic.setBackground(new Color(40, 160, 80));
                    btnStatic.setForeground(Color.WHITE);
                    btnStatic.setEnabled(true);
                } else {
                    btnStatic.setBackground(Color.GRAY);
                    btnStatic.setForeground(Color.DARK_GRAY);
                    btnStatic.setEnabled(false);
                }
                
                // Dynamic Analysis always available once uploaded
                btnDynamic.setBackground(new Color(200, 100, 30));
                btnDynamic.setForeground(Color.WHITE);
                btnDynamic.setEnabled(true);
            }
            return this;
        }
    }

    // Custom Button Editor
    class ButtonEditor extends DefaultCellEditor {
        protected JPanel panel;
        protected JButton btnStatic;
        protected JButton btnDynamic;
        private boolean isPushed;
        private int currentRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
            panel.setOpaque(true);
            panel.setBackground(Config.COLOR_ACCENT);

            btnStatic = new JButton("View Report");
            btnStatic.setFocusPainted(false);
            btnStatic.addActionListener(e -> {
                fireEditingStopped();
                if (currentFiles != null && currentFiles.size() > currentRow) {
                    UploadedFile uf = currentFiles.get(currentRow);
                    if ("Analysed".equals(uf.getStatus())) {
                        mainFrame.showReport(uf.getId());
                    }
                }
            });

            btnDynamic = new JButton("Dynamic Analysis");
            btnDynamic.setFocusPainted(false);
            btnDynamic.setBackground(new Color(200, 100, 30));
            btnDynamic.setForeground(Color.WHITE);
            btnDynamic.addActionListener(e -> {
                fireEditingStopped();
                if (currentFiles != null && currentFiles.size() > currentRow) {
                    UploadedFile uf = currentFiles.get(currentRow);
                    AnalysisController.runDynamicAnalysis(uf.getId(), DashboardScreen.this);
                }
            });

            panel.add(btnStatic);
            panel.add(btnDynamic);
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            currentRow = row;
            isPushed = true;
            return panel;
        }

        public Object getCellEditorValue() {
            isPushed = false;
            return "Actions";
        }

        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}
