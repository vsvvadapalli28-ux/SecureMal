package com.securemal.ui;

import com.securemal.config.Config;
import com.securemal.controllers.AnalysisController;
import com.securemal.models.UploadedFile;
import com.securemal.models.User;
import com.securemal.services.FileManagementService;
import com.securemal.state.AppState;
import com.securemal.ui.components.Icons;

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
    private JTextField txtSelectedFile;

    public DashboardScreen(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.fileService = new FileManagementService();

        setBackground(Config.COLOR_CONTENT);
        setLayout(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Config.COLOR_HEADER);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel(Icons.FILE_ICON + " SecureMal - Malware Analysis Sandbox");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        headerPanel.add(title, BorderLayout.WEST);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        userPanel.setBackground(Config.COLOR_HEADER);

        lblWelcome = new JLabel("Welcome, User");
        lblWelcome.setForeground(Color.WHITE);
        lblWelcome.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JButton btnLogout = new JButton("Logout");
        btnLogout.setBackground(Config.COLOR_BUTTON);
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setOpaque(true);
        btnLogout.setBorderPainted(false);
        btnLogout.setFocusPainted(false);
        btnLogout.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnLogout.setMargin(new Insets(5, 15, 5, 15));
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> {
            AppState.getInstance().logout();
            mainFrame.showLogin();
        });

        userPanel.add(lblWelcome);
        userPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        userPanel.add(btnLogout);
        headerPanel.add(userPanel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Center Content
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(Config.COLOR_CONTENT);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Upload Panel
        JPanel uploadPanel = new JPanel(new GridBagLayout());
        uploadPanel.setBackground(Config.COLOR_CONTENT);
        uploadPanel.setBorder(BorderFactory.createTitledBorder("File Upload"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel lblSelectedFile = new JLabel("Selected File:");
        lblSelectedFile.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        uploadPanel.add(lblSelectedFile, gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtSelectedFile = new JTextField(30);
        txtSelectedFile.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSelectedFile.setEditable(false);
        uploadPanel.add(txtSelectedFile, gbc);

        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JButton btnBrowse = new JButton("Browse");
        btnBrowse.setBackground(Config.COLOR_BUTTON);
        btnBrowse.setForeground(Color.WHITE);
        btnBrowse.setOpaque(true);
        btnBrowse.setBorderPainted(false);
        btnBrowse.setFocusPainted(false);
        btnBrowse.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnBrowse.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBrowse.addActionListener(e -> performUpload());
        uploadPanel.add(btnBrowse, gbc);

        centerPanel.add(uploadPanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Status Table
        String[] columns = { "#", "File Name", "Size", "Uploaded", "Risk", "Status", "Action", "Delete" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only Action column is editable (for button click)
            }
        };

        fileTable = new JTable(tableModel);
        fileTable.setBackground(Config.COLOR_CONTENT);
        fileTable.setForeground(Color.BLACK);
        fileTable.setRowHeight(35);
        fileTable.getTableHeader().setBackground(Config.COLOR_HEADER);
        fileTable.getTableHeader().setForeground(Color.WHITE);
        fileTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        fileTable.getTableHeader().setOpaque(true);

        // Force header colors via custom renderer
        javax.swing.table.DefaultTableCellRenderer headerRenderer =
                new javax.swing.table.DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(
                            JTable table, Object value, boolean isSelected,
                            boolean hasFocus, int row, int column) {
                        JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                                table, value, isSelected, hasFocus, row, column);
                        lbl.setBackground(Config.COLOR_HEADER);
                        lbl.setForeground(Color.WHITE);
                        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
                        lbl.setOpaque(true);
                        lbl.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(50, 70, 100)),
                                BorderFactory.createEmptyBorder(0, 8, 0, 8)));
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                        return lbl;
                    }
                };
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            fileTable.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }
        fileTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        fileTable.setSelectionBackground(new Color(200, 220, 240));
        fileTable.setSelectionForeground(Color.BLACK);

        // Custom Row Renderer for alternating colors
        fileTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 245, 245));
                }
                return c;
            }
        });

        // Custom Status Renderer
        fileTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                String status = value != null ? value.toString() : "Pending";
                if ("Pending".equals(status)) {
                    label.setText(Icons.PENDING_ICON + " Pending");
                } else if ("Analysed".equals(status)) {
                    label.setText(Icons.DONE_ICON + " Analysed");
                } else {
                    label.setText(status);
                }
                return label;
            }
        });

        // Action Column Button Renderer/Editor
        fileTable.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer());
        fileTable.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox()));

        // Delete Column Button Renderer/Editor
        int deleteCol = table.getColumnModel().getColumnCount() - 1;
        fileTable.getColumn("Delete").setCellRenderer(new DeleteButtonRenderer());
        fileTable.getColumn("Delete").setCellEditor(
            new DeleteButtonEditor(new JCheckBox()));
        fileTable.getColumnModel().getColumn(deleteCol).setPreferredWidth(90);
        fileTable.getColumnModel().getColumn(deleteCol).setMaxWidth(90);

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

        // Set minimum column widths
        fileTable.getColumnModel().getColumn(0).setMaxWidth(40);   // #
        fileTable.getColumnModel().getColumn(2).setPreferredWidth(90);  // Size
        fileTable.getColumnModel().getColumn(3).setPreferredWidth(140); // Uploaded
        fileTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // Risk
        fileTable.getColumnModel().getColumn(5).setPreferredWidth(80);  // Status
        fileTable.getColumnModel().getColumn(6).setMinWidth(230);        // Action — wide enough for both buttons
        fileTable.getColumnModel().getColumn(6).setPreferredWidth(230);

        JScrollPane scrollPane = new JScrollPane(fileTable);
        scrollPane.getViewport().setBackground(Config.COLOR_CONTENT);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Analysis Status"));
        centerPanel.add(scrollPane);

        add(centerPanel, BorderLayout.CENTER);
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
        if (currentUser == null)
            return;

        tableModel.setRowCount(0);
        Object[] loadingRow = { "", "Loading files...", "", "", "", "", "", "" };
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
                                "View Report",
                                Icons.DELETE_ICON + " Delete"
                        };
                        tableModel.addRow(rowData);
                    }
                    fileTable.revalidate();
                    fileTable.repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                    tableModel.setRowCount(0);
                    fileTable.revalidate();
                    fileTable.repaint();
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
            txtSelectedFile.setText(selectedFile.getAbsolutePath());
            User currentUser = AppState.getInstance().getCurrentUser();

            // Show progress dialog
            JDialog progressDialog = new JDialog(mainFrame, "Uploading", true);
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            JLabel lblUploading = new JLabel("Uploading file, please wait...");
            lblUploading.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            progressDialog.add(lblUploading, BorderLayout.NORTH);
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
                                        "Run Dynamic Analysis (VM Sandbox) instead?", "Dynamic Analysis",
                                        JOptionPane.YES_NO_OPTION);
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

    // Custom Button Renderer — always paints colors explicitly on every call
    class ButtonRenderer extends JPanel implements TableCellRenderer {
        private JButton btnStatic;
        private JButton btnDynamic;

        public ButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 4, 3));
            setOpaque(true);

            btnStatic = new JButton("View Report");
            btnStatic.setFocusPainted(false);
            btnStatic.setOpaque(true);
            btnStatic.setBorderPainted(false);
            btnStatic.setFont(new Font("Segoe UI", Font.BOLD, 10));

            btnDynamic = new JButton("Dynamic Analysis");
            btnDynamic.setFocusPainted(false);
            btnDynamic.setOpaque(true);
            btnDynamic.setBorderPainted(false);
            btnDynamic.setBackground(Config.COLOR_BUTTON);
            btnDynamic.setForeground(Color.WHITE);
            btnDynamic.setFont(new Font("Segoe UI", Font.BOLD, 10));

            add(btnStatic);
            add(btnDynamic);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            // Always repaint panel background explicitly
            Color rowBg = (row % 2 == 0) ? Color.WHITE : new Color(245, 245, 245);
            setBackground(isSelected ? table.getSelectionBackground() : rowBg);

            boolean isAnalysed = false;
            if (currentFiles != null && currentFiles.size() > row) {
                UploadedFile uf = currentFiles.get(row);
                isAnalysed = "Analysed".equals(uf.getStatus());
            }

            // View Report — orange when analysed, grey when pending — always explicit
            if (isAnalysed) {
                btnStatic.setBackground(Config.COLOR_BUTTON);
                btnStatic.setForeground(Color.WHITE);
                btnStatic.setEnabled(true);
            } else {
                btnStatic.setBackground(new Color(200, 200, 200));
                btnStatic.setForeground(new Color(100, 100, 100));
                btnStatic.setEnabled(false);
            }

            // Dynamic Analysis — always orange
            btnDynamic.setBackground(Config.COLOR_BUTTON);
            btnDynamic.setForeground(Color.WHITE);
            btnDynamic.setEnabled(true);

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
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 3));
            panel.setOpaque(true);
            panel.setBackground(Color.WHITE);

            btnStatic = new JButton("View Report");
            btnStatic.setFocusPainted(false);
            btnStatic.setOpaque(true);
            btnStatic.setBorderPainted(false);
            btnStatic.setFont(new Font("Segoe UI", Font.BOLD, 10));
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
            btnDynamic.setOpaque(true);
            btnDynamic.setBorderPainted(false);
            btnDynamic.setBackground(Config.COLOR_BUTTON);
            btnDynamic.setForeground(Color.WHITE);
            btnDynamic.setFont(new Font("Segoe UI", Font.BOLD, 10));
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

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = row;

            // Sync enabled state with current data
            boolean isAnalysed = currentFiles != null && currentFiles.size() > row
                    && "Analysed".equals(currentFiles.get(row).getStatus());
            if (isAnalysed) {
                btnStatic.setBackground(Config.COLOR_BUTTON);
                btnStatic.setForeground(Color.WHITE);
                btnStatic.setEnabled(true);
            } else {
                btnStatic.setBackground(new Color(200, 200, 200));
                btnStatic.setForeground(new Color(100, 100, 100));
                btnStatic.setEnabled(false);
            }
            btnDynamic.setBackground(Config.COLOR_BUTTON);
            btnDynamic.setForeground(Color.WHITE);

            isPushed = true;
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            isPushed = false;
            return "Actions";
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    private class DeleteButtonRenderer extends JButton implements TableCellRenderer {
        public DeleteButtonRenderer() {
            setText(Icons.DELETE_ICON + " Delete");
            setOpaque(true);
            setBackground(new Color(139, 0, 0));  // dark red
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setFocusPainted(false);
            setBorderPainted(false);
        }
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            return this;
        }
    }

    private class DeleteButtonEditor extends DefaultCellEditor {
        private JButton button;
        private int fileId;
        private String filename;
        private boolean clicked;
        public DeleteButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton(Icons.DELETE_ICON + " Delete");
            button.setOpaque(true);
            button.setBackground(new Color(139, 0, 0));
            button.setForeground(Color.WHITE);
            button.setFont(new Font("Segoe UI", Font.BOLD, 12));
            button.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));

            button.addActionListener(e -> {
                if (clicked) {
                    // Show confirmation dialog
                    int confirm = JOptionPane.showConfirmDialog(
                        DashboardScreen.this,
                        "<html>Are you sure you want to delete:<br>"
                        + "<b>" + filename + "</b><br><br>"
                        + "This will permanently remove the file and its analysis report.<br>"
                        + "This action cannot be undone.</html>",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                    );
                    if (confirm == JOptionPane.YES_OPTION) {
                        int fId = fileId;
                        new SwingWorker<Boolean, Void>() {
                            @Override
                            protected Boolean doInBackground() {
                                return fileService.deleteFile(fId);
                            }
                            @Override
                            protected void done() {
                                try {
                                    boolean success = get();
                                    if (success) {
                                        refreshTable();
                                    } else {
                                        JOptionPane.showMessageDialog(
                                            DashboardScreen.this,
                                            "Failed to delete the file. Please try again.",
                                            "Delete Error",
                                            JOptionPane.ERROR_MESSAGE
                                        );
                                    }
                                } catch (Exception ex) {
                                    System.err.println("Delete error: " + ex.getMessage());
                                }
                            }
                        }.execute();
                    }
                }
                clicked = false;
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int column) {
            // Get fileId and filename from currentFiles for this row
            if (currentFiles != null && currentFiles.size() > row) {
                UploadedFile uf = currentFiles.get(row);
                fileId = uf.getId();
                filename = uf.getOriginalFilename();
            }
            clicked = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return Icons.DELETE_ICON + " Delete";
        }
    }
}
