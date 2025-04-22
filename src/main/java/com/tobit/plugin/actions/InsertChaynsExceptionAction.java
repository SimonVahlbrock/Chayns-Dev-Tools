package com.tobit.plugin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.tobit.plugin.controller.ChaynsExceptionController;
import com.tobit.plugin.services.TokenService;
import com.tobit.plugin.views.InsertChaynsExceptionPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class InsertChaynsExceptionAction extends AnAction {

    public InsertChaynsExceptionAction() {
        super("Insert Chayns Exception");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final Project project = e.getRequiredData(CommonDataKeys.PROJECT);

        ChaynsExceptionController controller = new ChaynsExceptionController(project);

        if (!controller.hasNamespace()) {
            // Show error notification instead of dialog
            controller.showErrorDialog(null,
                    "No ChaynsErrors namespace found. Add a namespace in appsettings.json to create exceptions.",
                    "Missing Configuration");
            return;
        }

        // Create and show dialog with the panel
        InsertExceptionDialog dialog = new InsertExceptionDialog(project, editor, controller);
        dialog.show();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable the action if:
        // 1. There is an editor
        // 2. There is a project
        // 3. User is logged in (has a token)
        // 4. The namespace is set
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Project project = e.getProject();

        boolean enabled = false;
        if (editor != null && project != null &&
                !TokenService.getInstance(project).getToken().isEmpty()) {
            ChaynsExceptionController controller = new ChaynsExceptionController(project);
            enabled = controller.hasNamespace();
        }

        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private static class InsertExceptionDialog extends DialogWrapper {
        private final InsertChaynsExceptionPanel exceptionPanel;

        public InsertExceptionDialog(Project project, Editor editor, ChaynsExceptionController controller) {
            super(project, true);
            exceptionPanel = new InsertChaynsExceptionPanel(controller, editor);
            setTitle("Insert Chayns Exception");
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            return exceptionPanel.getPanel();
        }

        @Override
        protected Action @NotNull [] createActions() {
            // Only show cancel button, the insert is handled by the panel's submit button
            return new Action[] { };
        }
    }
}