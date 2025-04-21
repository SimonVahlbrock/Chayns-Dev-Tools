package com.tobit.plugin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class InsertChaynsExceptionAction extends AnAction {

    private static final List<String> EXCEPTION_TYPES = Arrays.asList(
            "InvalidParameterException",
            "ResourceNotFoundException",
            "AccessDeniedException",
            "ServiceUnavailableException"
    );

    public InsertChaynsExceptionAction() {
        super("Insert Chayns Exception", "Insert a ChaynsException at cursor position", null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final Project project = e.getRequiredData(CommonDataKeys.PROJECT);

        JBPopupFactory.getInstance()
                .createListPopup(new BaseListPopupStep<>("Select Exception Type", EXCEPTION_TYPES) {
                    @Override
                    public PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {
                        if (finalChoice) {
                            insertException(editor, project, selectedValue);
                        }
                        return FINAL_CHOICE;
                    }

                    @Override
                    public @Nullable Icon getIconFor(String value) {
                        return null;
                    }
                })
                .showInBestPositionFor(editor);
    }

    private void insertException(Editor editor, Project project, String exceptionType) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            int offset = editor.getCaretModel().getOffset();
            String exceptionCode = "throw new ChaynsException(\"" + exceptionType + "\");";
            editor.getDocument().insertString(offset, exceptionCode);
            editor.getCaretModel().moveToOffset(offset + exceptionCode.length());
        });
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
}