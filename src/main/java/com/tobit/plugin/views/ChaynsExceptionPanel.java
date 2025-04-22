package com.tobit.plugin.views;

import com.intellij.ui.JBColor;
import com.tobit.plugin.controller.ChaynsExceptionController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ChaynsExceptionPanel {
    private final JPanel panel = new JPanel(new BorderLayout());
    private final JLabel namespaceLabel = new JLabel();
    private final ChaynsExceptionController controller;
    private final JPanel contentPanel = new JPanel();

    public ChaynsExceptionPanel(ChaynsExceptionController controller) {
        this.controller = controller;
        setupUI();
    }

    public JPanel getPanel() {
        return panel;
    }

    public void updateData(String namespace) {
        contentPanel.removeAll();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        if (namespace == null || namespace.isEmpty()) {
            // Use a vertical layout for the error message
            JPanel messagePanel = new JPanel();
            messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
            messagePanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));

            JLabel messageLabel = new JLabel("No namespace found in appsettings.json");
            messageLabel.setForeground(JBColor.RED);

            // Add spacing between labels
            messagePanel.add(messageLabel);
            messagePanel.add(Box.createRigidArea(new Dimension(0, 10)));

            contentPanel.add(messagePanel);
        } else {
            namespaceLabel.setText("Namespace: " + namespace);
            // Add any other content for when namespace is available, positioned vertically
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void setupUI() {
        // Main panel with vertical box layout
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Panel with namespace info
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        namespaceLabel.setForeground(JBColor.GRAY);
        namespaceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(namespaceLabel);

        // Add spacing between namespace label and docs link
        headerPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Documentation link
        JLabel docsLink = new JLabel("View error codes documentation");
        docsLink.setForeground(JBColor.BLUE);
        docsLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        docsLink.setAlignmentX(Component.LEFT_ALIGNMENT);
        docsLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                controller.showDocumentation();
            }
        });
        headerPanel.add(docsLink);

        // Set up main layout with components stacked vertically
        panel.removeAll();
        panel.add(headerPanel);

        // Content panel for dynamic content
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(contentPanel);
    }

    public void reloadData() {
        controller.reloadData();
    }
}