package com.tobit.plugin.controller;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.tobit.plugin.models.ChaynsExceptionModel;
import com.tobit.plugin.models.data.ApiResponse;
import com.tobit.plugin.models.data.ExceptionItem;
import com.tobit.plugin.services.TokenService;
import com.tobit.plugin.views.ChaynsExceptionPanel;
import com.tobit.plugin.views.InsertChaynsExceptionPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ChaynsExceptionController {
    private final ChaynsExceptionModel model;
    private ChaynsExceptionPanel view;
    private final Project project;
    private final TokenService tokenService;

    public ChaynsExceptionController(Project project) {
        this.project = project;
        this.model = new ChaynsExceptionModel(project);
        this.tokenService = TokenService.getInstance(project);

        model.addDataChangeListener((namespaces, selectedNamespace) -> {
            if (view != null) {
                view.updateData(namespaces, selectedNamespace);
            }
        });
    }

    public ChaynsExceptionPanel createView() {
        view = new ChaynsExceptionPanel(this);
        view.updateData(model.getNamespaces(), model.getSelectedNamespace());
        return view;
    }

    public InsertChaynsExceptionPanel createInsertPanel(Editor editor) {
        return new InsertChaynsExceptionPanel(this, editor);
    }

    public void insertException(Editor editor, String code) {
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
     * Validates that a string is in snake_case format.
     */
    public boolean isValidSnakeCase(String text) {
        return text.matches("^[a-z][a-z0-9]*(_[a-z0-9]+)*$");
    }

    /**
     * Converts a snake_case string to PascalCase
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

    public List<String> getNamespaces() {
        return model.getNamespaces();
    }

    public String getSelectedNamespace() {
        return model.getSelectedNamespace();
    }

    public void setSelectedNamespace(String namespace) {
        model.setSelectedNamespace(namespace);
    }

    public ApiResponse createException(ExceptionItem exceptionItem) {
        String token = tokenService.getTobitDevToken();
        return model.createException(exceptionItem, token);
    }

    public void showDocumentation() {
        // This could be moved to a util class in a larger application
        com.intellij.ide.BrowserUtil.browse("https://dev.tobit.com/errorCodes");
    }

    public void showErrorDialog(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public boolean hasNamespace() {
        return model.hasNamespaces();
    }
}