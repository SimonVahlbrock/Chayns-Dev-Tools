package com.tobit.plugin.views;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.tobit.plugin.controller.ChaynsExceptionController;
import com.tobit.plugin.models.data.ApiResponse;
import com.tobit.plugin.models.data.ExceptionItem;
import com.tobit.plugin.services.ApiService;
import com.tobit.plugin.services.TokenService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collections;

public class InsertChaynsExceptionPanel {
    private final JPanel panel = new JPanel(new GridBagLayout());
    private final Project project;
    private final Editor editor;
    private final ChaynsExceptionController controller;
    private final TokenService tokenService;

    private final JBTextField nameField = new JBTextField();
    private final JBTextField statusCodeField = new JBTextField();
    private final JComboBox<String> logLevelCombo = new JComboBox<>(new String[]{"Info", "Warn", "Error"});
    private final JBTextField descriptionField = new JBTextField();
    private final JBTextField messageField = new JBTextField();
    private final JButton submitButton = new JButton("Insert Exception");

    private final ApiService apiService = new ApiService();

    public InsertChaynsExceptionPanel(Project project, Editor editor, ChaynsExceptionController controller) {
        this.project = project;
        this.editor = editor;
        this.controller = controller;
        this.tokenService = TokenService.getInstance(project);
        setupUI();
    }

    public JPanel getPanel() {
        return panel;
    }

    private void setupUI() {
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setMinimumSize(new Dimension(300, 0)); // Set minimum width to 300px (200px plus some padding)
        panel.setPreferredSize(new Dimension(400, 300)); // Set preferred dimensions

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.2;

        // Name field with tooltip
        panel.add(new JBLabel("Name:"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.8;
        nameField.setToolTipText("snake_case. Namespace is automatically loaded.");
        panel.add(nameField, constraints);

        // Status Code field
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0.2;
        panel.add(new JBLabel("Statuscode:"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.8;
        statusCodeField.setToolTipText("Enter a numeric HTTP status code");
        panel.add(statusCodeField, constraints);

        // Log Level combo
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 0.2;
        panel.add(new JBLabel("LogLevel:"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.8;
        panel.add(logLevelCombo, constraints);

        // Description field with tooltip
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.weightx = 0.2;
        panel.add(new JBLabel("Description:"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.8;
        descriptionField.setToolTipText("Describe the error code for the developer.");
        panel.add(descriptionField, constraints);

        // Message field with tooltip
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.weightx = 0.2;
        panel.add(new JBLabel("Message:"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.8;
        messageField.setToolTipText("A new textstring is automatically created for the error code. The texts can then be adjusted in the textstring administration. Parameters must be specified in the following format: ##text##. Language is german");
        panel.add(messageField, constraints);

        // Submit button
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.CENTER;
        submitButton.addActionListener(this::onSubmit);
        panel.add(submitButton, constraints);
    }

    private void onSubmit(ActionEvent e) {
        try {
            String name = nameField.getText().trim();

            // Validate input
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Exception name is required", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Validate snake_case format
            if (!isSnakeCase(name)) {
                JOptionPane.showMessageDialog(panel, "Exception name must be in snake_case format (e.g., invalid_token)",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String description = descriptionField.getText().trim();
            if (description.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Description is required", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String message = messageField.getText().trim();
            if (message.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Message is required", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int statusCode;
            try {
                statusCode = Integer.parseInt(statusCodeField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Status code must be a valid number", "Validation Error", JOptionPane.ERROR_MESSAGE);
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

            // Call API to create the exception
            var response = createException(newException);

            if (!response.isSuccess()) {
                var errorMessage = "Error creating exception: " + response.data();
                if (response.data().contains("chayns/auth/package/at_least_one_group_required")) {
                    errorMessage = "You are not allowed to create an exception. Contact an administrator of dev.tobit.com";
                }
                JOptionPane.showMessageDialog(panel,
                        errorMessage,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }


            // Insert the exception in editor
            controller.insertException(editor, project, newException.code());

            // Close the dialog
            Window window = SwingUtilities.getWindowAncestor(panel);
            if (window != null) {
                window.dispose();
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(panel,
                    "Error creating exception: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Validates that a string is in snake_case format.
     * snake_case is all lowercase letters with words separated by underscores.
     */
    private boolean isSnakeCase(String text) {
        // Regex pattern for snake_case: lowercase letters, numbers and underscores only
        // Must not start or end with underscore, and no consecutive underscores
        return text.matches("^[a-z][a-z0-9]*(_[a-z0-9]+)*$");
    }

    private ApiResponse createException(ExceptionItem exceptionItem) {
        String token = tokenService.getTobitDevToken();

        // In a real implementation, this would call the API to create the exception
        String apiUrl = "https://webapi.tobit.com/chaynserrors/v1/Codes";
        String namespace = controller.getNamespace();

        // Build JSON payload
        String jsonBody = String.format(
                "{\"code\":\"%s%s\",\"statusCode\":%d,\"description\":\"%s\",\"logLevel\":%d,\"textGer\":\"%s\"}",
                namespace,
                exceptionItem.code(),
                exceptionItem.statusCode(),
                exceptionItem.description(),
                exceptionItem.LogLevel(),
                exceptionItem.message()
        );

        // This is just a placeholder - in a real implementation, we would handle the response

        return apiService.postRequest(apiUrl, jsonBody, Collections.singletonMap("Authorization", "Bearer " + token));
    }
}