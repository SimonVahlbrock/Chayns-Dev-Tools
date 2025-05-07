package com.tobit.plugin.views;

import com.intellij.ui.JBColor;
import com.tobit.plugin.controller.ChaynsExceptionController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class ChaynsExceptionPanel {
    private final JPanel rootPanel = new JPanel();
    private final JPanel namespacesPanel = new JPanel();
    private final JPanel contentPanel = new JPanel();
    private final ChaynsExceptionController controller;

    public ChaynsExceptionPanel(ChaynsExceptionController controller) {
        this.controller = controller;
        initializeUI();
    }

    public JPanel getPanel() {
        return rootPanel;
    }

    public void updateData(List<String> namespaces, String selectedNamespace) {
        contentPanel.removeAll();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        namespacesPanel.removeAll();
        namespacesPanel.setLayout(new BoxLayout(namespacesPanel, BoxLayout.Y_AXIS));

        if (namespaces == null || namespaces.isEmpty()) {
            displayEmptyNamespacesMessage();
        } else {
            displayNamespacesList(namespaces, selectedNamespace);
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void displayEmptyNamespacesMessage() {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 10));

        JLabel messageLabel = new JLabel("No namespaces found in appsettings.json");
        messageLabel.setForeground(JBColor.RED);
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        messagePanel.add(messageLabel);
        messagePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(messagePanel);
    }

    private void displayNamespacesList(List<String> namespaces, String selectedNamespace) {
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

    private void initializeUI() {
        // Configure root panel with vertical box layout
        rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.Y_AXIS));

        // Create header panel
        JPanel headerPanel = createHeaderPanel();

        // Configure content panel
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // Add panels to main layout
        rootPanel.add(headerPanel);
        rootPanel.add(contentPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        // Instruction label
        JLabel instructionLabel = new JLabel("Use ALT+SHIFT+X or Right-click to insert new chayns exception.");
        instructionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        instructionLabel.setForeground(JBColor.GRAY);
        Font smallerFont = instructionLabel.getFont().deriveFont(instructionLabel.getFont().getSize() - 1f);
        instructionLabel.setFont(smallerFont);
        headerPanel.add(instructionLabel);

        // Documentation link
        JLabel docsLink = new JLabel("Edit and delete exceptions");
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

        return headerPanel;
    }

    public void reloadData() {
        controller.reloadData();
    }
}