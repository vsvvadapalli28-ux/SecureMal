package com.securemal;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.securemal.ui.MainFrame;

/**
 * Main Entry Point for the SecureMal Application.
 */
public class Main {
    
    /**
     * Main method. Sets LookAndFeel, creates the App State, and runs the UI
     * on the Swing Event Dispatch Thread (EDT).
     * 
     * @param args Command Line Arguments (unused)
     */
    public static void main(String[] args) {
        // Enhance look and feel for a more modern native aesthetic
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException 
               | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            System.err.println("Failed to initialize System Look and Feel");
        }

        SwingUtilities.invokeLater(() -> {
            // Set global font
            java.awt.Font appFont = new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13);
            java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.getDefaults().get(key);
                if (value instanceof java.awt.Font) {
                    UIManager.put(key, appFont);
                }
            }

            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}
