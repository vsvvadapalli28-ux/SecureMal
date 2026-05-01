package com.securemal.ui;

import com.securemal.config.AppConfig;
import com.securemal.config.Config;
import com.securemal.controllers.AnalysisController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.List;

/**
 * Modal dialog that either guides the user through installing VirtualBox
 * (when not found) or lets them select and configure the VM to use for
 * dynamic analysis (when VirtualBox is installed).
 */
public class VirtualBoxSetupDialog extends JDialog {

    private static final Color BG_DARK  = new Color(13, 13, 26);
    private static final Color BG_CARD  = new Color(20, 28, 48);
    private static final Color BORDER   = new Color(40, 60, 100);
    private static final Color ACCENT   = new Color(70, 130, 200);
    private static final Color AMBER    = new Color(243, 156, 18);
    private static final Color TEXT_DIM = new Color(160, 160, 176);

    /**
     * Shows the appropriate dialog:
     * — VM selector when VirtualBox is installed
     * — Install guide when VirtualBox is NOT found
     */
    public static void show(Frame parent) {
        VirtualBoxSetupDialog dialog = new VirtualBoxSetupDialog(parent);
        dialog.setVisible(true);
    }

    private VirtualBoxSetupDialog(Frame parent) {
        super(parent, "Dynamic Analysis — VM Configuration", true);
        setSize(660, 600);
        setLocationRelativeTo(parent);
        setResizable(true);
        getContentPane().setBackground(BG_DARK);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(BG_DARK);
        root.setBorder(new EmptyBorder(24, 28, 24, 28));

        if (AppConfig.isVirtualBoxPathValid()) {
            buildVmSelectorPanel(root, parent);
        } else {
            buildInstallGuidePanel(root);
        }

        JScrollPane scroll = new JScrollPane(root);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        setContentPane(scroll);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Panel A: VirtualBox is found — show VM selector
    // ──────────────────────────────────────────────────────────────────────

    private void buildVmSelectorPanel(JPanel root, Frame parent) {
        // Title
        JLabel title = new JLabel("⚙  Configure Dynamic Analysis VM");
        title.setFont(new Font("Segoe UI", Font.BOLD, 19));
        title.setForeground(ACCENT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createRigidArea(new Dimension(0, 6)));

        JLabel subtitle = new JLabel(
            "<html><body style='width:560px; font-family:Segoe UI; font-size:12px; color:#a0a0b0;'>"
            + "VirtualBox is installed. Select which registered VM to use for dynamic analysis, "
            + "and confirm the clean-state snapshot name."
            + "</body></html>"
        );
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(subtitle);
        root.add(Box.createRigidArea(new Dimension(0, 20)));

        // ── VM ComboBox section ──────────────────────────────────────────
        JPanel vmCard = makeCard();
        root.add(vmCard);
        root.add(Box.createRigidArea(new Dimension(0, 14)));

        JLabel vmHeading = new JLabel("Registered VMs");
        vmHeading.setFont(new Font("Segoe UI", Font.BOLD, 13));
        vmHeading.setForeground(Color.WHITE);
        vmHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        vmCard.add(vmHeading);
        vmCard.add(Box.createRigidArea(new Dimension(0, 8)));

        // Status label
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setForeground(TEXT_DIM);
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ComboBox
        JComboBox<String> vmCombo = new JComboBox<>();
        vmCombo.setBackground(new Color(26, 35, 58));
        vmCombo.setForeground(Color.WHITE);
        vmCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        vmCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        vmCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Refresh button
        JButton refreshBtn = new JButton("↻  Refresh");
        styleSecondaryBtn(refreshBtn);
        refreshBtn.addActionListener(e -> populateVmCombo(vmCombo, statusLabel));

        JPanel comboRow = new JPanel(new BorderLayout(8, 0));
        comboRow.setOpaque(false);
        comboRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        comboRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        comboRow.add(vmCombo, BorderLayout.CENTER);
        comboRow.add(refreshBtn, BorderLayout.EAST);
        vmCard.add(comboRow);
        vmCard.add(Box.createRigidArea(new Dimension(0, 6)));
        vmCard.add(statusLabel);

        // ── No VMs banner ────────────────────────────────────────────────
        JPanel noVmBanner = new JPanel(new BorderLayout(10, 0));
        noVmBanner.setBackground(new Color(70, 40, 0));
        noVmBanner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, AMBER),
            new EmptyBorder(10, 12, 10, 12)
        ));
        noVmBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
        noVmBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        noVmBanner.setVisible(false);

        JLabel noVmLabel = new JLabel(
            "<html><body style='font-family:Segoe UI; font-size:12px; color:#ffd080;'>"
            + "<b>No VMs found.</b> You need to create and register a VM first. "
            + "See the commands below, or run <code>setup_vm.bat</code> from the project folder."
            + "</body></html>"
        );
        noVmBanner.add(noVmLabel, BorderLayout.CENTER);
        root.add(noVmBanner);
        root.add(Box.createRigidArea(new Dimension(0, 10)));

        // Initial population
        populateVmCombo(vmCombo, statusLabel);
        // After populating, show banner if empty
        if (vmCombo.getItemCount() == 0) {
            noVmBanner.setVisible(true);
        }
        vmCombo.addActionListener(e -> noVmBanner.setVisible(vmCombo.getItemCount() == 0));

        // Pre-select current saved VM
        String savedVm = AppConfig.getVmName();
        for (int i = 0; i < vmCombo.getItemCount(); i++) {
            if (savedVm.equals(vmCombo.getItemAt(i))) {
                vmCombo.setSelectedIndex(i);
                break;
            }
        }

        // ── Snapshot name ────────────────────────────────────────────────
        JPanel snapCard = makeCard();
        root.add(snapCard);
        root.add(Box.createRigidArea(new Dimension(0, 14)));

        JLabel snapHeading = new JLabel("Clean-state Snapshot Name");
        snapHeading.setFont(new Font("Segoe UI", Font.BOLD, 13));
        snapHeading.setForeground(Color.WHITE);
        snapHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        snapCard.add(snapHeading);
        snapCard.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel snapHint = new JLabel(
            "<html><body style='font-family:Segoe UI; font-size:11px; color:#a0a0b0;'>"
            + "The VM must have a snapshot taken from a clean OS state. "
            + "SecureMal will restore to this snapshot before each analysis run."
            + "</body></html>"
        );
        snapHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        snapCard.add(snapHint);
        snapCard.add(Box.createRigidArea(new Dimension(0, 8)));

        JTextField snapField = new JTextField(AppConfig.getSnapshotName());
        styleTextField(snapField);
        snapCard.add(snapField);

        // ── VBoxManage path ──────────────────────────────────────────────
        JPanel pathCard = makeCard();
        root.add(pathCard);
        root.add(Box.createRigidArea(new Dimension(0, 14)));

        JLabel pathHeading = new JLabel("VBoxManage Executable Path");
        pathHeading.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pathHeading.setForeground(Color.WHITE);
        pathHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        pathCard.add(pathHeading);
        pathCard.add(Box.createRigidArea(new Dimension(0, 8)));

        JTextField pathField = new JTextField(AppConfig.getVirtualBoxPath());
        styleTextField(pathField);
        pathField.setFont(new Font("Monospaced", Font.PLAIN, 11));
        pathCard.add(pathField);

        // ── Quick-start commands ─────────────────────────────────────────
        if (vmCombo.getItemCount() == 0) {
            root.add(makeCommandGuide());
            root.add(Box.createRigidArea(new Dimension(0, 14)));
        }

        // ── Buttons row ──────────────────────────────────────────────────
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(sep);
        root.add(Box.createRigidArea(new Dimension(0, 14)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton saveBtn = new JButton("Save Configuration");
        saveBtn.setBackground(Config.COLOR_HIGHLIGHT);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setOpaque(true);
        saveBtn.setBorderPainted(false);
        saveBtn.setFocusPainted(false);
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> {
            String selected = (String) vmCombo.getSelectedItem();
            String snap  = snapField.getText().trim();
            String vpath = pathField.getText().trim();

            if (selected == null || selected.isBlank()) {
                JOptionPane.showMessageDialog(this,
                    "Please select a VM from the list (or create one first).",
                    "No VM Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (snap.isBlank()) {
                JOptionPane.showMessageDialog(this,
                    "Snapshot name cannot be empty.",
                    "Invalid Input", JOptionPane.WARNING_MESSAGE);
                return;
            }

            AppConfig.setVmName(selected);
            AppConfig.setSnapshotName(snap);
            if (!vpath.isBlank()) {
                AppConfig.setVirtualBoxPath(vpath);
            }

            JOptionPane.showMessageDialog(this,
                "Configuration saved!\n\nVM: " + selected + "\nSnapshot: " + snap,
                "Saved", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        });

        JButton closeBtn = new JButton("Close");
        styleSecondaryBtn(closeBtn);
        closeBtn.addActionListener(e -> dispose());

        btnRow.add(saveBtn);
        btnRow.add(closeBtn);
        root.add(btnRow);
    }

    /** Populates the VM combo asynchronously from VBoxManage list vms. */
    private void populateVmCombo(JComboBox<String> combo, JLabel status) {
        combo.removeAllItems();
        status.setText("Loading registered VMs…");
        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return AnalysisController.getAvailableVMs();
            }
            @Override
            protected void done() {
                try {
                    List<String> vms = get();
                    if (vms.isEmpty()) {
                        status.setText("No registered VMs found.");
                    } else {
                        for (String vm : vms) {
                            combo.addItem(vm);
                        }
                        status.setText(vms.size() + " VM(s) found.");
                    }
                } catch (Exception ex) {
                    status.setText("Could not query VMs: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Panel B: VirtualBox is NOT found — install guide
    // ──────────────────────────────────────────────────────────────────────

    private void buildInstallGuidePanel(JPanel root) {
        JLabel title = new JLabel("⚙  VirtualBox Not Found");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(AMBER);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createRigidArea(new Dimension(0, 6)));

        JLabel subtitle = new JLabel(
            "<html><body style='width:560px; font-family:Segoe UI; font-size:12px; color:#a0a0b0;'>"
            + "Dynamic analysis runs the file inside an isolated VirtualBox VM and monitors its behaviour. "
            + "VirtualBox was not detected on this machine. Follow the steps below to set it up."
            + "</body></html>"
        );
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(subtitle);
        root.add(Box.createRigidArea(new Dimension(0, 20)));

        root.add(makeStep("Step 1 — Download VirtualBox",
            "Visit virtualbox.org and download the Windows installer.",
            "https://www.virtualbox.org/wiki/Downloads", "Open Download Page"));
        root.add(Box.createRigidArea(new Dimension(0, 12)));

        root.add(makeStep("Step 2 — Install VirtualBox",
            "Run the installer and complete setup with default options.", null, null));
        root.add(Box.createRigidArea(new Dimension(0, 12)));

        root.add(makeStep("Step 3 — Create a clean Windows VM",
            "Create a Windows 10/11 VM, then take a snapshot of the clean state.\n"
            + "You can run setup_vm.bat from the project folder to automate this.", null, null));
        root.add(Box.createRigidArea(new Dimension(0, 12)));

        root.add(makeStep("Step 4 — Set the VBoxManage path",
            "Default location:  C:\\Program Files\\Oracle\\VirtualBox\\VBoxManage.exe\n"
            + "Enter the path below and click Save:", null, null));
        root.add(Box.createRigidArea(new Dimension(0, 8)));

        JPanel pathRow = new JPanel(new BorderLayout(8, 0));
        pathRow.setOpaque(false);
        pathRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        pathRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JTextField pathField = new JTextField(AppConfig.getVirtualBoxPath());
        styleTextField(pathField);
        pathField.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JButton saveBtn = new JButton("Save");
        saveBtn.setBackground(Config.COLOR_BUTTON);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setOpaque(true);
        saveBtn.setBorderPainted(false);
        saveBtn.setFocusPainted(false);
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> {
            String p = pathField.getText().trim();
            if (!p.isEmpty()) {
                AppConfig.setVirtualBoxPath(p);
                JOptionPane.showMessageDialog(this,
                    "Path saved! Restart the application and try Dynamic Analysis again.",
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
        });

        pathRow.add(pathField, BorderLayout.CENTER);
        pathRow.add(saveBtn, BorderLayout.EAST);
        root.add(pathRow);
        root.add(Box.createRigidArea(new Dimension(0, 20)));

        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(sep);
        root.add(Box.createRigidArea(new Dimension(0, 16)));

        JLabel note = new JLabel(
            "<html><body style='width:560px; font-family:Segoe UI; font-size:11px; color:#a0a0b0;'>"
            + "<b style='color:#e0e0e0;'>Note:</b> Static analysis is always available without VirtualBox. "
            + "Use \"Run Analysis\" from the dashboard to generate a report based on file content, "
            + "strings, entropy, and PE headers."
            + "</body></html>"
        );
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(note);
        root.add(Box.createRigidArea(new Dimension(0, 20)));

        JButton closeBtn = new JButton("Close");
        styleSecondaryBtn(closeBtn);
        closeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        closeBtn.addActionListener(e -> dispose());
        root.add(closeBtn);
    }

    // ──────────────────────────────────────────────────────────────────────
    // VBoxManage command guide (shown when no VMs are registered)
    // ──────────────────────────────────────────────────────────────────────

    private JPanel makeCommandGuide() {
        JPanel card = makeCard();

        JLabel heading = new JLabel("Quick-Start: Create a VM with VBoxManage");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 13));
        heading.setForeground(AMBER);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(heading);
        card.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel hint = new JLabel(
            "<html><body style='font-family:Segoe UI; font-size:11px; color:#a0a0b0;'>"
            + "No VMs are registered. You can either run <b>setup_vm.bat</b> from the project folder, "
            + "or run these commands manually in a terminal:"
            + "</body></html>"
        );
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(hint);
        card.add(Box.createRigidArea(new Dimension(0, 8)));

        String commands =
            "# 1. Create the VM\n"
            + "VBoxManage createvm --name \"SecureMal-Clean\" --ostype Windows10_64 --register\n\n"
            + "# 2. Configure hardware\n"
            + "VBoxManage modifyvm \"SecureMal-Clean\" --memory 2048 --cpus 2 --vram 64\n\n"
            + "# 3. Create a 50 GB disk and attach it\n"
            + "VBoxManage createhd --filename \"SecureMal-Clean.vdi\" --size 51200\n"
            + "VBoxManage storagectl \"SecureMal-Clean\" --name SATA --add sata\n"
            + "VBoxManage storageattach \"SecureMal-Clean\" --storagectl SATA --port 0 --device 0 --type hdd --medium \"SecureMal-Clean.vdi\"\n\n"
            + "# 4. Attach a Windows ISO and start\n"
            + "VBoxManage storageattach \"SecureMal-Clean\" --storagectl SATA --port 1 --device 0 --type dvddrive --medium \"C:\\path\\to\\windows.iso\"\n"
            + "VBoxManage startvm \"SecureMal-Clean\"\n\n"
            + "# 5. After installing Windows and configuring, take snapshot:\n"
            + "VBoxManage snapshot \"SecureMal-Clean\" take \"Clean\" --description \"Clean baseline\"";

        JTextArea cmdArea = new JTextArea(commands);
        cmdArea.setEditable(false);
        cmdArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        cmdArea.setForeground(new Color(180, 220, 180));
        cmdArea.setBackground(new Color(10, 20, 10));
        cmdArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        cmdArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane cmdScroll = new JScrollPane(cmdArea);
        cmdScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        cmdScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        cmdScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        card.add(cmdScroll);

        return card;
    }

    // ──────────────────────────────────────────────────────────────────────
    // UI helpers
    // ──────────────────────────────────────────────────────────────────────

    private JPanel makeCard() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(true);
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(12, 16, 12, 16)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));
        return panel;
    }

    private void styleTextField(JTextField field) {
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        field.setBackground(new Color(26, 35, 58));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void styleSecondaryBtn(JButton btn) {
        btn.setBackground(new Color(40, 60, 100));
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private JPanel makeStep(String heading, String body, String linkUrl, String linkLabel) {
        JPanel panel = makeCard();

        JLabel h = new JLabel(heading);
        h.setFont(new Font("Segoe UI", Font.BOLD, 13));
        h.setForeground(Color.WHITE);
        h.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(h);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));

        String htmlBody = "<html><body style='width:520px; font-family:Segoe UI; font-size:11px; color:#a0a0b0;'>"
            + body.replace("\n", "<br>") + "</body></html>";
        JLabel b = new JLabel(htmlBody);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(b);

        if (linkUrl != null && linkLabel != null) {
            panel.add(Box.createRigidArea(new Dimension(0, 6)));
            JLabel link = new JLabel("<html><a style='color:#4da6ff;'>" + linkLabel + "</a></html>");
            link.setCursor(new Cursor(Cursor.HAND_CURSOR));
            link.setAlignmentX(Component.LEFT_ALIGNMENT);
            link.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        Desktop.getDesktop().browse(new URI(linkUrl));
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(VirtualBoxSetupDialog.this,
                            "Visit: " + linkUrl, "Browser unavailable", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            });
            panel.add(link);
        }

        return panel;
    }
}
