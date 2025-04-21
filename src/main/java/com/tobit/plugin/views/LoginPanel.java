package com.tobit.plugin.views;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import com.tobit.plugin.controller.LoginController;
import com.tobit.plugin.services.TokenService;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that handles user authentication through chayns login.
 * Provides a simple interface for logging in and displays current login status.
 */
public class LoginPanel implements TokenService.TokenChangeListener {
    private final JPanel panel = new JPanel(new BorderLayout());
    private final LoginController controller;
    private JButton loginButton;
    private final TokenService tokenService;

    public LoginPanel(Project project) {
        this.controller = new LoginController(project);
        this.tokenService = TokenService.getInstance(project);

        controller.addTokenChangeListener(this);
        tokenService.addTokenChangeListener(this);
        setupUI();
    }

    /**
     * Returns the configured login panel for display
     */
    public JPanel getPanel() {
        return panel;
    }

    /**
     * Initializes and configures the UI components
     */
    private void setupUI() {
        // Create main content panel with vertical layout
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(JBUI.Borders.empty(20));

        // Create and configure header components
        JLabel welcomeLabel = createWelcomeLabel();
        JLabel descriptionLabel = createDescriptionLabel();

        // Configure the login button
        loginButton = createLoginButton();

        // Assemble the panel with proper spacing
        assembleContentPanel(contentPanel, welcomeLabel, descriptionLabel, loginButton);

        // Center the content in the main panel
        panel.add(contentPanel, BorderLayout.CENTER);

        // Set initial UI state based on token availability
        updateLoginButtonState(tokenService.getToken());
    }

    /**
     * Creates a styled welcome header label
     */
    private JLabel createWelcomeLabel() {
        JLabel label = new JLabel("Welcome to chayns Developer Tools");
        Font font = label.getFont().deriveFont(Font.BOLD, 18f);
        label.setFont(font);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    /**
     * Creates a description label with instructions
     */
    private JLabel createDescriptionLabel() {
        JLabel label = new JLabel("Login with your chayns account to get started");
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    /**
     * Creates and configures the login button with action listener
     */
    private JButton createLoginButton() {
        JButton button = new JButton("Login with chayns");
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(e -> {
            button.setEnabled(false);
            button.setText("Logging in...");

            // Run login process asynchronously to prevent UI freezing
            SwingUtilities.invokeLater(() -> {
                boolean success = controller.getToken();
                if (!success) {
                    showError("Remote login failed or was cancelled");
                    resetLoginButton();
                }
            });
        });
        return button;
    }

    /**
     * Assembles all components in the content panel with proper spacing
     */
    private void assembleContentPanel(
            JPanel contentPanel,
            JLabel welcomeLabel,
            JLabel descriptionLabel,
            JButton loginButton
    ) {
        // Attempt to add logo spacing (logo loading handled elsewhere)
        try {
            contentPanel.add(Box.createVerticalStrut(20));
        } catch (Exception e) {
            // Continue without logo spacing if there's an issue
        }

        // Add components with vertical spacing between them
        contentPanel.add(welcomeLabel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(descriptionLabel);
        contentPanel.add(Box.createVerticalStrut(30));
        contentPanel.add(loginButton);
    }

    /**
     * Displays an error message to the user
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(panel, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Resets the login button to its default state
     */
    private void resetLoginButton() {
        loginButton.setEnabled(true);
        loginButton.setText("Login with chayns");
    }

    /**
     * Updates the login button appearance based on authentication status
     */
    private void updateLoginButtonState(String token) {
        if (token.isEmpty()) {
            resetLoginButton();
        } else {
            loginButton.setEnabled(false);
            loginButton.setText("Logged in");
        }
    }

    /**
     * TokenChangeListener implementation.
     * Updates UI when authentication status changes
     */
    @Override
    public void onTokenChanged(String newToken) {
        SwingUtilities.invokeLater(() -> {
            updateLoginButtonState(newToken);
        });
    }
}