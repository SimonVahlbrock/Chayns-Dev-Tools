package com.tobit.plugin.views;

import com.intellij.ui.JBColor;
import com.tobit.plugin.controller.ChaynsExceptionController;
import com.tobit.plugin.models.data.ExceptionItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class ChaynsExceptionPanel {
    private final JPanel panel = new JPanel(new BorderLayout());
    private final JLabel namespaceLabel = new JLabel();
    private final ChaynsExceptionController controller;

    public ChaynsExceptionPanel(ChaynsExceptionController controller) {
        this.controller = controller;
        setupUI();
    }

    public JPanel getPanel() {
        return panel;
    }

    public void updateData(String namespace, List<ExceptionItem> exceptionTypes) {
        namespaceLabel.setText("Namespace: " + namespace);
    }

    private void setupUI() {
        // Panel with namespace info
        JPanel namespacePanel = new JPanel(new BorderLayout());
        namespacePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        namespaceLabel.setForeground(JBColor.GRAY);
        namespacePanel.add(namespaceLabel, BorderLayout.NORTH);

        // Documentation link
        JLabel docsLink = new JLabel("View error codes documentation");
        docsLink.setForeground(JBColor.BLUE);
        docsLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        docsLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                controller.showDocumentation();
            }
        });
        namespacePanel.add(docsLink, BorderLayout.SOUTH);

        // Main layout
        panel.add(namespacePanel, BorderLayout.NORTH);

        // Empty panel for the center to maintain layout
        JPanel emptyPanel = new JPanel();
        panel.add(emptyPanel, BorderLayout.CENTER);
    }

    public void reloadData() {
        controller.reloadData();
    }
}