package com.securemal;

import com.securemal.ui.MainFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

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
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}
