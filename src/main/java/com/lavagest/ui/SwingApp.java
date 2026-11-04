package com.lavagest.ui;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

public class SwingApp {
    public static void main(String[] args) {
        // Aceleración gráfica por hardware 2D y eliminación de parpadeo (anti-glitch)
        System.setProperty("sun.java2d.d3d", "true");
        System.setProperty("sun.java2d.noddraw", "true");
        System.setProperty("sun.awt.noerasebackground", "true");
        System.setProperty("flatlaf.useWindowDecorations", "false"); // Marco nativo Windows (ultra fluido a 60 FPS)

        // Inicializar Look & Feel moderno FlatLaf
        try {
            FlatLightLaf.setup();
            RepaintManager.currentManager(null).setDoubleBufferingEnabled(true);
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
