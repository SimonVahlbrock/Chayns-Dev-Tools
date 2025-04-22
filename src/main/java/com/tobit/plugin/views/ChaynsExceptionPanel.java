package com.tobit.plugin.views;

import com.intellij.ui.JBColor;
import com.tobit.plugin.controller.ChaynsExceptionController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class ChaynsExceptionPanel {
    private final JPanel panel = new JPanel(new BorderLayout());
    private final JPanel namespacesPanel = new JPanel();
    private final ChaynsExceptionController controller;
    private final JPanel contentPanel = new JPanel();

    public ChaynsExceptionPanel(ChaynsExceptionController controller) {
        this.controller = controller;
        setupUI();
    }

    public JPanel getPanel() {
        return panel;
    }

    public void updateData(List<String> namespaces, String selectedNamespace) {
        contentPanel.removeAll();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        namespacesPanel.removeAll();
        namespacesPanel.setLayout(new BoxLayout(namespacesPanel, BoxLayout.Y_AXIS));

        if (namespaces == null || namespaces.isEmpty()) {
            // Use a vertical layout for the error message
            JPanel messagePanel = new JPanel();
            messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
            messagePanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));

            JLabel messageLabel = new JLabel("No namespaces found in appsettings.json");
            messageLabel.setForeground(JBColor.RED);

            // Add spacing between labels
            messagePanel.add(messageLabel);
            messagePanel.add(Box.createRigidArea(new Dimension(0, 10)));

            contentPanel.add(messagePanel);
        } else {
            // Display all namespaces
            JLabel titleLabel = new JLabel("Available Namespaces:");
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            namespacesPanel.add(titleLabel);
            namespacesPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            for (String namespace : namespaces) {
                JLabel namespaceLabel = new JLabel("• " + namespace);
                namespaceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                if (namespace.equals(selectedNamespace)) {
                    namespaceLabel.setFont(namespaceLabel.getFont().deriveFont(Font.BOLD));
                }
                namespacesPanel.add(namespaceLabel);
                namespacesPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }

            contentPanel.add(namespacesPanel);
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
        panel.add(headerPanel, BorderLayout.NORTH);

        // Content panel for dynamic content
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(contentPanel, BorderLayout.CENTER);
    }

    public void reloadData() {
        controller.reloadData();
    }
}