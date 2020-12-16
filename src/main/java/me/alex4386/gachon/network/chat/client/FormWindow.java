package me.alex4386.gachon.network.chat.client;

import javax.swing.*;

public class FormWindow {
    private JPanel panel;
    private JFrame frame = null;

    private String title;

    public FormWindow(JPanel panel, String title) {
        this.panel = panel;
        this.title = title;
    }

    public void open() {
        if (frame == null) {
            frame = new JFrame(this.title);
            frame.setContentPane(this.panel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
        }

        frame.setVisible(true);
    }

    public boolean isOpened() {
        if (frame == null) return false;
        return frame.isVisible();
    }

    public void close() {
        frame.setVisible(false);
    }

    public JPanel getPanel() {
        return panel;
    }

}

