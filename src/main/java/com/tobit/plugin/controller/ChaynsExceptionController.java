package com.tobit.plugin.controller;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.tobit.plugin.models.ChaynsExceptionModel;
import com.tobit.plugin.models.data.ExceptionItem;
import com.tobit.plugin.views.ChaynsExceptionPanel;

import java.util.List;

public class ChaynsExceptionController {
    private final ChaynsExceptionModel model;
    private ChaynsExceptionPanel view;

    public ChaynsExceptionController(Project project) {
        this.model = new ChaynsExceptionModel(project);

        model.addDataChangeListener((namespace, exceptionTypes) -> {
            if (view != null) {
                view.updateData(namespace, exceptionTypes);
            }
        });
    }

    public ChaynsExceptionPanel createView() {
        view = new ChaynsExceptionPanel(this);
        view.updateData(model.getNamespace(), model.getExceptionTypes());
        return view;
    }

    public void insertException(Editor editor, Project project, String code) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            int offset = editor.getCaretModel().getOffset();

            // Convert snake_case to PascalCase
            String pascalCaseCode = convertToPascalCase(code);

            String exceptionCode = String.format("throw new %sException();", pascalCaseCode);
            editor.getDocument().insertString(offset, exceptionCode);
            editor.getCaretModel().moveToOffset(offset + exceptionCode.length());
        });
    }

    /**
     * Converts a snake_case string to PascalCase
     * For example: "invalid_token" becomes "InvalidToken"
     */
    private String convertToPascalCase(String snakeCase) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }

        return result.toString();
    }

    public void reloadData() {
        model.reload();
    }

    public List<ExceptionItem> getExceptionTypes() {
        return model.getExceptionTypes();
    }

    public String getNamespace() {
        return model.getNamespace();
    }
}