package com.securemal.ui;

import java.awt.CardLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.securemal.config.Config;

public class MainFrame extends JFrame {
    
    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    
    private final LoginScreen loginScreen;
    private final RegisterScreen registerScreen;
    private final DashboardScreen dashboardScreen;

    public MainFrame() {
        setTitle(Config.APP_TITLE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(Config.COLOR_BG_DARK);
        
        loginScreen = new LoginScreen(this);
        registerScreen = new RegisterScreen(this);
        dashboardScreen = new DashboardScreen(this);
        
        mainPanel.add(loginScreen, "LOGIN");
        mainPanel.add(registerScreen, "REGISTER");
        mainPanel.add(dashboardScreen, "DASHBOARD");
        
        add(mainPanel);
    }
    
    public void showLogin() {
        cardLayout.show(mainPanel, "LOGIN");
    }
    
    public void showRegister() {
        cardLayout.show(mainPanel, "REGISTER");
    }
    
    public void showDashboard() {
        cardLayout.show(mainPanel, "DASHBOARD");
    }
    
    private ReportViewerScreen reportScreen;
    private JPanel cardPanel;

    public void showReport(int fileId) {
        System.out.println("DEBUG: showReport called with fileId: " + fileId);
        // FIX D: Remove old report screen if exists
        for (java.awt.Component c : mainPanel.getComponents()) {
            if ("REPORT".equals(c.getName())) {
                mainPanel.remove(c);
                break;
            }
        }

        reportScreen = new ReportViewerScreen(this, fileId);
        reportScreen.setName("REPORT");
        mainPanel.add(reportScreen, "REPORT");
        cardLayout.show(mainPanel, "REPORT");

        // FIX D: Force full layout pass to give the new screen its actual pixel size
        mainPanel.revalidate();
        mainPanel.repaint();
        getContentPane().revalidate();
        getContentPane().repaint();
    }
}
