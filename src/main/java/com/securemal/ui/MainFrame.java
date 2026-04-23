package com.securemal.ui;

import com.securemal.config.Config;
import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    
    private CardLayout cardLayout;
    private JPanel mainPanel;
    
    private LoginScreen loginScreen;
    private RegisterScreen registerScreen;
    private DashboardScreen dashboardScreen;

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
    
    public void showReport(int fileId) {
        ReportViewerScreen reportScreen = new ReportViewerScreen(this, fileId);
        mainPanel.add(reportScreen, "REPORT");
        cardLayout.show(mainPanel, "REPORT");
    }
}
