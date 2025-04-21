package com.tobit.plugin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.tobit.plugin.controller.SitesController;
import com.tobit.plugin.services.TokenService;
import com.tobit.plugin.services.ViewManager;
import com.tobit.plugin.views.SitesPanel;
import org.jetbrains.annotations.NotNull;

public class SearchSiteAction extends AnAction {
    public SearchSiteAction() {
        super("Search Sites");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return;

        Project project = e.getProject();
        if (project == null) return;

        String selectedText = editor.getSelectionModel().getSelectedText();
        if (selectedText == null) return;

        if (!selectedText.trim().isEmpty()) {
            // Show the chayns tool window
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            ToolWindow toolWindow = toolWindowManager.getToolWindow("Chayns Dev Tools");
            if (toolWindow == null) return;

            toolWindow.show(() -> {
                // Get the view from ViewManager
                ViewManager viewManager = ViewManager.getInstance(project);
                SitesPanel sitesView = viewManager.getView(SitesPanel.class);

                if (sitesView != null) {
                    sitesView.searchForText(selectedText.trim());
                } else {
                    // Fallback if view not found
                    SitesController controller = new SitesController(project);
                    SitesPanel view = controller.createView();
                    view.searchForText(selectedText.trim());
                }
            });
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable the action if:
        // 1. There is selected text
        // 2. There is a project
        // 3. User is logged in (has a token)
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Project project = e.getProject();
        boolean hasSelection = editor != null && editor.getSelectionModel().hasSelection();

        e.getPresentation().setEnabled(hasSelection && project != null &&
                !TokenService.getInstance(project).getToken().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}