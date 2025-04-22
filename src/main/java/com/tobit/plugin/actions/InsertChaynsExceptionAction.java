package com.tobit.plugin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.tobit.plugin.controller.ChaynsExceptionController;
import com.tobit.plugin.views.InsertChaynsExceptionPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class InsertChaynsExceptionAction extends AnAction {

    public InsertChaynsExceptionAction() {
        super("Insert Chayns Exception", "Insert a ChaynsException at cursor position", null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final Project project = e.getRequiredData(CommonDataKeys.PROJECT);

        ChaynsExceptionController controller = new ChaynsExceptionController(project);

        // Create and show dialog with the new panel
        InsertExceptionDialog dialog = new InsertExceptionDialog(project, editor, controller);
        dialog.show();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        e.getPresentation().setEnabledAndVisible(editor != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private static class InsertExceptionDialog extends DialogWrapper {
        private final InsertChaynsExceptionPanel exceptionPanel;

        public InsertExceptionDialog(Project project, Editor editor, ChaynsExceptionController controller) {
            super(project, true);
            exceptionPanel = new InsertChaynsExceptionPanel(project, editor, controller);
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
            return new Action[] { getCancelAction() };
        }
    }
}