package com.tobit.plugin.views;

import com.intellij.openapi.editor.Editor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.tobit.plugin.controller.ChaynsExceptionController;
import com.tobit.plugin.models.data.ApiResponse;
import com.tobit.plugin.models.data.ExceptionItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class InsertChaynsExceptionPanel {
    private final JPanel panel = new JPanel(new GridBagLayout());
    private final Editor editor;
    private final ChaynsExceptionController controller;

    private JComboBox<String> namespaceCombo;
    private final JBTextField nameField = new JBTextField();
    private final JBTextField statusCodeField = new JBTextField();
    private final JComboBox<String> logLevelCombo = new JComboBox<>(new String[]{"Info", "Warn", "Error"});
    private final JBTextField descriptionField = new JBTextField();
    private final JBTextField messageField = new JBTextField();
    private final JButton submitButton = new JButton("Insert Exception");
    private final JLabel namespaceHintLabel = new JLabel();

    public InsertChaynsExceptionPanel(ChaynsExceptionController controller, Editor editor) {
        this.editor = editor;
        this.controller = controller;

        if (controller.hasNamespace()) {
            setupUI();
        } else {
            setupNoNamespaceUI();
        }
    }

    private void setupNoNamespaceUI() {
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setMinimumSize(new Dimension(300, 0));
        panel.setPreferredSize(new Dimension(400, 150));

        JLabel messageLabel = new JLabel("No namespace found in appsettings.json");
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel instructionLabel = new JLabel("Add a ChaynsErrors section with Namespaces to continue.");
        instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);

        panel.setLayout(new GridLayout(2, 1));
        panel.add(messageLabel);
        panel.add(instructionLabel);
    }

    public JPanel getPanel() {
        return panel;
    }

    private void setupUI() {
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setMinimumSize(new Dimension(300, 0));
        panel.setPreferredSize(new Dimension(400, 330));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.2;

        List<String> namespaces = controller.getNamespaces();
        boolean hasMultipleNamespaces = namespaces.size() > 1;
        int currentRow = 0;

        // Only show namespace dropdown if there are multiple namespaces
        if (hasMultipleNamespaces) {
            // Namespace selection dropdown
            panel.add(new JBLabel("Namespace:"), constraints);
            constraints.gridx = 1;
            constraints.weightx = 0.8;

            // Create and populate namespace dropdown
            namespaceCombo = new JComboBox<>(namespaces.toArray(new String[0]));
            if (!namespaces.isEmpty()) {
                namespaceCombo.setSelectedItem(controller.getSelectedNamespace());
            }
            namespaceCombo.addActionListener(e ->
                    controller.setSelectedNamespace((String) namespaceCombo.getSelectedItem())
            );
            panel.add(namespaceCombo, constraints);
            currentRow++;
        }

        // Name field with tooltip
        constraints.gridx = 0;
        constraints.gridy = currentRow++;
        constraints.weightx = 0.2;
        panel.add(new JBLabel("Name:"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.8;
        nameField.setToolTipText("snake_case. Namespace is automatically loaded.");
        panel.add(nameField, constraints);

        // Status Code field
        constraints.gridx = 0;
        constraints.gridy = currentRow++;
        constraints.weightx = 0.2;
        panel.add(new JBLabel("Statuscode:"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.8;
        statusCodeField.setToolTipText("Enter a numeric HTTP status code");
        panel.add(statusCodeField, constraints);

        // Log Level combo
        constraints.gridx = 0;
        constraints.gridy = currentRow++;
        constraints.weightx = 0.2;
        panel.add(new JBLabel("LogLevel:"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.8;
        panel.add(logLevelCombo, constraints);

        // Description field with tooltip
        constraints.gridx = 0;
        constraints.gridy = currentRow++;
        constraints.weightx = 0.2;
        panel.add(new JBLabel("Description:"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.8;
        descriptionField.setToolTipText("Describe the error code for the developer.");
        panel.add(descriptionField, constraints);

        // Message field with tooltip
        constraints.gridx = 0;
        constraints.gridy = currentRow++;
        constraints.weightx = 0.2;
        panel.add(new JBLabel("Message:"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.8;
        messageField.setToolTipText("A new textstring is automatically created for the error code. The texts can then be adjusted in the textstring administration. Parameters must be specified in the following format: ##text##. Language is german");
        panel.add(messageField, constraints);

        // If there's only one namespace, show a hint above the submit button
        if (!hasMultipleNamespaces && !namespaces.isEmpty()) {
            constraints.gridx = 0;
            constraints.gridy = currentRow++;
            constraints.gridwidth = 2;
            constraints.anchor = GridBagConstraints.CENTER;

            String namespace = controller.getSelectedNamespace();
            namespaceHintLabel.setText("Using namespace: " + namespace);
            namespaceHintLabel.setForeground(new Color(100, 100, 100)); // Gray color for the hint
            namespaceHintLabel.setFont(namespaceHintLabel.getFont().deriveFont(Font.ITALIC));
            panel.add(namespaceHintLabel, constraints);
        }

        // Submit button
        constraints.gridx = 0;
        constraints.gridy = currentRow;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.CENTER;
        submitButton.addActionListener(this::onSubmit);
        panel.add(submitButton, constraints);
    }

    private void onSubmit(ActionEvent e) {
        try {
            String name = nameField.getText().trim();

            // Validate input - using controller for validation logic
            if (name.isEmpty()) {
                controller.showErrorDialog(panel, "Exception name is required", "Validation Error");
                return;
            }

            // Validate snake_case format - using controller for validation
            if (!controller.isValidSnakeCase(name)) {
                controller.showErrorDialog(panel,
                        "Exception name must be in snake_case format (e.g., invalid_token)",
                        "Validation Error");
                return;
            }

            String description = descriptionField.getText().trim();
            if (description.isEmpty()) {
                controller.showErrorDialog(panel, "Description is required", "Validation Error");
                return;
            }

            String message = messageField.getText().trim();
            if (message.isEmpty()) {
                controller.showErrorDialog(panel, "Message is required", "Validation Error");
                return;
            }

            int statusCode;
            try {
                statusCode = Integer.parseInt(statusCodeField.getText().trim());
            } catch (NumberFormatException ex) {
                controller.showErrorDialog(panel, "Status code must be a valid number", "Validation Error");
                return;
            }

            int logLevel = logLevelCombo.getSelectedIndex() + 2; // 2=Info, 3=Warn, 4=Error

            // Create ExceptionItem
            ExceptionItem newException = new ExceptionItem(
                    name,
                    description,
                    statusCode,
                    logLevel,
                    message
            );

            // Call controller to create the exception
            ApiResponse response = controller.createException(newException);

            if (!response.isSuccess()) {
                var errorMessage = "Error creating exception: " + response.data();
                if (response.data().contains("chayns/auth/package/at_least_one_group_required")) {
                    errorMessage = "You are not allowed to create an exception. Contact an administrator of dev.tobit.com";
                }
                controller.showErrorDialog(panel, errorMessage, "Error");
                return;
            }

            // Insert the exception in editor
            controller.insertException(editor, newException.code());

            // Close the dialog
            Window window = SwingUtilities.getWindowAncestor(panel);
            if (window != null) {
                window.dispose();
            }

        } catch (Exception ex) {
            controller.showErrorDialog(panel,
                    "Error creating exception: " + ex.getMessage(),
                    "Error");
        }
    }
}