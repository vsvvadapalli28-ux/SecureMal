package com.securemal.ui;

import com.securemal.auth.AuthService;
import com.securemal.config.Config;
import com.securemal.models.User;
import com.securemal.state.AppState;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LoginScreen extends JPanel {
    private final MainFrame mainFrame;
    private final JTextField txtUsername;
    private final JPasswordField txtPassword;
    private final JLabel lblStatus;
    private final JButton btnLogin;

    public LoginScreen(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setBackground(Config.COLOR_BG_DARK);
        setLayout(new GridBagLayout());
        
        JPanel card = new JPanel();
        card.setBackground(Config.COLOR_BG_DARK);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(40, 60, 40, 60));
        
        // Title
        JLabel title = new JLabel("SecureMal");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(Config.COLOR_TEXT_WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Subtitle
        JLabel subtitle = new JLabel("Offline Malware Sandbox");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Inputs
        txtUsername = createStyledTextField("Email");
        txtPassword = createStyledPasswordField();
        
        // Status Label
        lblStatus = new JLabel(" ");
        lblStatus.setForeground(Color.RED);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Button
        btnLogin = new JButton("Login");
        btnLogin.setBackground(Color.decode("#e84393"));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setOpaque(true);
        btnLogin.setBorderPainted(false);
        btnLogin.setFocusPainted(false);
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(300, 40));
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnLogin.addActionListener(e -> performLogin());
        
        // Register Link
        JLabel lblRegister = new JLabel("<html><u>Don't have an account? Register</u></html>");
        lblRegister.setForeground(Config.COLOR_TEXT_WHITE);
        lblRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblRegister.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblRegister.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                lblStatus.setText(" ");
                mainFrame.showRegister();
            }
        });
        
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(subtitle);
        card.add(Box.createRigidArea(new Dimension(0, 30)));
        card.add(createFieldRow("Email", txtUsername));
        card.add(Box.createRigidArea(new Dimension(0, 15)));
        card.add(createFieldRow("Password", txtPassword));
        card.add(Box.createRigidArea(new Dimension(0, 15)));
        card.add(lblStatus);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(btnLogin);
        card.add(Box.createRigidArea(new Dimension(0, 20)));
        card.add(lblRegister);
        
        add(card);
    }
    
    private JPanel createFieldRow(final String labelText, JComponent field) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(360, 40));
        row.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl = new JLabel(labelText);
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setPreferredSize(new Dimension(75, 40));
        lbl.setMinimumSize(new Dimension(75, 40));
        lbl.setMaximumSize(new Dimension(75, 40));
        lbl.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(lbl);
        row.add(Box.createRigidArea(new Dimension(8, 0)));
        row.add(field);
        return row;
    }

    private JTextField createStyledTextField(@SuppressWarnings("unused") final String placeholder) {
        JTextField field = new JTextField();
        field.setMaximumSize(new Dimension(300, 40));
        field.setPreferredSize(new Dimension(300, 40));
        field.setBackground(Config.COLOR_ACCENT);
        field.setForeground(Config.COLOR_TEXT_WHITE);
        field.setCaretColor(Config.COLOR_TEXT_WHITE);
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Config.COLOR_ACCENT.darker()),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        return field;
    }
    
    private JPasswordField createStyledPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setMaximumSize(new Dimension(300, 40));
        field.setPreferredSize(new Dimension(300, 40));
        field.setBackground(Config.COLOR_ACCENT);
        field.setForeground(Config.COLOR_TEXT_WHITE);
        field.setCaretColor(Config.COLOR_TEXT_WHITE);
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Config.COLOR_ACCENT.darker()),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        return field;
    }
    
    private void performLogin() {
        final String username = txtUsername.getText();
        final String password = new String(txtPassword.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            lblStatus.setText("Please enter both fields.");
            lblStatus.setForeground(Color.RED);
            return;
        }
        
        btnLogin.setEnabled(false);
        lblStatus.setText("Logging in...");
        lblStatus.setForeground(Color.GRAY);
        
        SwingWorker<User, Void> worker = new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                AuthService authService = new AuthService();
                return authService.loginUser(username, password);
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                try {
                    final User user = get();
                    if (user != null) {
                        lblStatus.setText(" ");
                        AppState.getInstance().setCurrentUser(user);
                        txtPassword.setText(""); // clear password
                        mainFrame.showDashboard();
                    } else {
                        lblStatus.setForeground(Color.RED);
                        lblStatus.setText("Invalid username or password.");
                    }
                } catch (Exception ex) {
                    lblStatus.setForeground(Color.RED);
                    lblStatus.setText("An error occurred during login.");
                    System.err.println("Login error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }
}
