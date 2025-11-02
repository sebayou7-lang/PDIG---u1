package main;

import javax.swing.SwingUtilities;

import vista.ventana;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ventana v = new ventana();
            v.setVisible(true);
        });
    }
}
