package com.securemal.ui;

import com.securemal.auth.AuthService;
import com.securemal.config.Config;
import com.securemal.models.User;
import com.securemal.state.AppState;
import com.securemal.ui.components.Icons;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RegisterScreen extends JPanel {
    private MainFrame mainFrame;
    private JTextField txtUsername;
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;
    private JLabel lblStatus;
    private JButton btnRegister;

    public RegisterScreen(MainFrame mainFrame) {
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
        JLabel subtitle = new JLabel("Create an Account");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Inputs
        txtUsername = createStyledTextField("Username");
        txtEmail = createStyledTextField("Email");
        txtPassword = createStyledPasswordField();
        txtConfirmPassword = createStyledPasswordField();
        
        // Status Label
        lblStatus = new JLabel(" ");
        lblStatus.setForeground(Color.RED);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Button
        btnRegister = new JButton("Register");
        btnRegister.setBackground(Config.COLOR_HIGHLIGHT);
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setOpaque(true);
        btnRegister.setBorderPainted(false);
        btnRegister.setFocusPainted(false);
        btnRegister.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRegister.setMaximumSize(new Dimension(300, 40));
        btnRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnRegister.addActionListener(e -> performRegister());
        
        // Login Link
        JLabel lblLogin = new JLabel("<html><u>Already have an account? Login</u></html>");
        lblLogin.setForeground(Config.COLOR_TEXT_WHITE);
        lblLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblLogin.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                lblStatus.setText(" ");
                mainFrame.showLogin();
            }
        });
        
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(subtitle);
        card.add(Box.createRigidArea(new Dimension(0, 30)));
        card.add(createLabel("Username"));
        card.add(txtUsername);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(createLabel("Email"));
        card.add(txtEmail);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(createLabel("Password"));
        card.add(txtPassword);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(createLabel("Confirm Password"));
        card.add(txtConfirmPassword);
        card.add(Box.createRigidArea(new Dimension(0, 15)));
        card.add(lblStatus);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(btnRegister);
        card.add(Box.createRigidArea(new Dimension(0, 20)));
        card.add(lblLogin);
        
        add(card);
    }
    
    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(Color.LIGHT_GRAY);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }
    
    private JTextField createStyledTextField(String placeholder) {
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
    
    private void performRegister() {
        String username = txtUsername.getText();
        String email = txtEmail.getText();
        String password = new String(txtPassword.getPassword());
        String confirmPassword = new String(txtConfirmPassword.getPassword());
        
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            lblStatus.setForeground(Color.RED);
            lblStatus.setText("Please fill out all fields.");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            lblStatus.setForeground(Color.RED);
            lblStatus.setText("Passwords do not match.");
            return;
        }
        
        if (password.length() < 6) {
            lblStatus.setForeground(Color.RED);
            lblStatus.setText("Password must be >= 6 characters.");
            return;
        }
        
        btnRegister.setEnabled(false);
        lblStatus.setForeground(Color.GRAY);
        lblStatus.setText("Creating account...");
        
        SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                AuthService authService = new AuthService();
                return authService.registerUser(username, email, password);
            }

            @Override
            protected void done() {
                btnRegister.setEnabled(true);
                try {
                    int result = get();
                    if (result == 1) {
                        lblStatus.setForeground(Color.GREEN);
                        lblStatus.setText("Account created!");
                        
                        Timer timer = new Timer(1500, e -> {
                            mainFrame.showLogin();
                            lblStatus.setText(" ");
                            txtUsername.setText("");
                            txtEmail.setText("");
                            txtPassword.setText("");
                            txtConfirmPassword.setText("");
                        });
                        timer.setRepeats(false);
                        timer.start();
                    } else if (result == -1) {
                        lblStatus.setForeground(Color.RED);
                        lblStatus.setText("Email already in use. Please use a different email.");
                    } else {
                        lblStatus.setForeground(Color.RED);
                        lblStatus.setText("Registration failed. Check all fields and try again.");
                    }
                } catch (Exception ex) {
                    lblStatus.setForeground(Color.RED);
                    lblStatus.setText("An error occurred during registration.");
                    ex.printStackTrace();
                }
            }
        };
        worker.execute();
    }
}
