package com.tobit.plugin.views;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.tobit.plugin.controller.PersonsController;
import com.tobit.plugin.controller.SitesController;
import com.tobit.plugin.services.TokenService;
import com.tobit.plugin.services.ViewManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ChaynsDevToolsWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ChaynsToolWindow myToolWindow = new ChaynsToolWindow(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(myToolWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);

        // Set the actions in the title
        toolWindow.setTitleActions(List.of(new RefreshAction(project), new LogoutAction(project)));
    }
}

class ChaynsToolWindow {
    private static final String LOGIN_CARD = "login";
    private static final String MAIN_CARD = "main";

    private final JPanel panel = new JPanel(new CardLayout());
    private final LoginPanel loginView;
    private final JPanel mainView = new JPanel(new BorderLayout());
    private final JBTabbedPane tabbedPane = new JBTabbedPane();

    private final PersonsPanel personsPanel;
    private final SitesPanel sitesPanel;

    public ChaynsToolWindow(Project project) {
        TokenService tokenService = TokenService.getInstance(project);
        this.loginView = new LoginPanel(project);

        // Initialize controllers
        // Your panels
        PersonsController personsController = new PersonsController(project);
        SitesController sitesController = new SitesController(project);

        // Create views
        this.personsPanel = personsController.createView();
        this.sitesPanel = sitesController.createView();

        setupUI();
        tokenService.addTokenChangeListener(new TokenService.TokenChangeListener() {
            @Override
            public void onTokenChanged(String newToken) {
                updateUI(!newToken.isEmpty());
            }
        });

        // Initial UI setup based on token status
        updateUI(!tokenService.getToken().isEmpty());

        // Register views with the view manager
        ViewManager.getInstance(project).registerView(PersonsPanel.class, personsPanel);
        ViewManager.getInstance(project).registerView(SitesPanel.class, sitesPanel);
    }

    private void setupUI() {
        // Setup main view with tabbed pane
        mainView.add(tabbedPane, BorderLayout.CENTER);

        // Add tabs to tabbed pane
        tabbedPane.addTab("Sites", sitesPanel.getPanel());
        tabbedPane.addTab("Persons", personsPanel.getPanel());

        // Add both views to card layout
        panel.add(loginView.getPanel(), LOGIN_CARD);
        panel.add(mainView, MAIN_CARD);
    }

    private void updateUI(boolean hasToken) {
        CardLayout layout = (CardLayout) panel.getLayout();
        if (hasToken) {
            layout.show(panel, MAIN_CARD);
        } else {
            layout.show(panel, LOGIN_CARD);
        }
    }

    public JPanel getContent() {
        return panel;
    }
}

class RefreshAction extends AnAction {
    private final Project project;

    public RefreshAction(Project project) {
        super("Reload Data", "Reload sites and persons data", AllIcons.Actions.Refresh);
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Reload both sites and persons data
        SitesPanel sitesPanel = ViewManager.getInstance(project).getView(SitesPanel.class);
        PersonsPanel personsPanel = ViewManager.getInstance(project).getView(PersonsPanel.class);

        if (sitesPanel != null) {
            sitesPanel.reloadData();
        }

        if (personsPanel != null) {
            personsPanel.reloadData();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable if user is logged in
        TokenService tokenService = TokenService.getInstance(project);
        e.getPresentation().setEnabled(!tokenService.getToken().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}

class LogoutAction extends AnAction {
    private final Project project;

    public LogoutAction(Project project) {
        super("Log Out", "Log out of chayns", AllIcons.Actions.Exit);
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        new LogoutDialog(project).show();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable if user is logged in
        TokenService tokenService = TokenService.getInstance(project);
        e.getPresentation().setEnabled(!tokenService.getToken().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}

class LogoutDialog extends DialogWrapper {
    private final Project project;

    public LogoutDialog(Project project) {
        super(project);
        this.project = project;
        setTitle("Log Out");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Are you sure you want to log out?");
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        // Clear token and notify listeners
        TokenService tokenService = TokenService.getInstance(project);
        tokenService.logout();
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }
}