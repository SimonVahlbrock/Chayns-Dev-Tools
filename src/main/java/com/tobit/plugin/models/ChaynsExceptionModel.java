package com.tobit.plugin.models;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.tobit.plugin.models.data.ExceptionItem;
import com.tobit.plugin.services.ApiService;
import com.tobit.plugin.models.data.ApiResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChaynsExceptionModel {
    private final Project project;
    private final List<String> namespaces = new ArrayList<>();
    private String selectedNamespace = "";
    private final ApiService apiService = new ApiService();

    private final List<DataChangeListener> dataChangeListeners = new ArrayList<>();

    public interface DataChangeListener {
        void onDataChanged(List<String> namespaces, String selectedNamespace);
    }

    public ChaynsExceptionModel(Project project) {
        this.project = project;
        loadNamespacesFromAppSettings();
    }

    public void addDataChangeListener(DataChangeListener listener) {
        dataChangeListeners.add(listener);
    }

    public void removeDataChangeListener(DataChangeListener listener) {
        dataChangeListeners.remove(listener);
    }

    private void notifyDataChanged() {
        for (DataChangeListener listener : dataChangeListeners) {
            listener.onDataChanged(namespaces, selectedNamespace);
        }
    }

    private void loadNamespacesFromAppSettings() {
        namespaces.clear();
        VirtualFile appSettingsFile = findFileInProject("appsettings.json");
        if (appSettingsFile != null) {
            try {
                String content = new String(appSettingsFile.contentsToByteArray(), StandardCharsets.UTF_8);
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(content, JsonObject.class);

                if (jsonObject.has("ChaynsErrors") &&
                        jsonObject.getAsJsonObject("ChaynsErrors").has("Namespaces")) {
                    JsonArray namespacesArray = jsonObject.getAsJsonObject("ChaynsErrors")
                            .getAsJsonArray("Namespaces");

                    for (JsonElement element : namespacesArray) {
                        namespaces.add(element.getAsString());
                    }

                    if (!namespaces.isEmpty()) {
                        selectedNamespace = namespaces.get(0);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        notifyDataChanged();
    }

    private VirtualFile findFileInProject(String fileName) {
        VirtualFile baseDir = project.getBaseDir();
        return findFileRecursively(baseDir, fileName);
    }

    private VirtualFile findFileRecursively(VirtualFile directory, String fileName) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return null;
        }

        // Check if file exists in current directory
        VirtualFile targetFile = directory.findChild(fileName);
        if (targetFile != null && targetFile.exists()) {
            return targetFile;
        }

        // Recursively search in subdirectories
        for (VirtualFile child : directory.getChildren()) {
            if (child.isDirectory()) {
                VirtualFile foundFile = findFileRecursively(child, fileName);
                if (foundFile != null) {
                    return foundFile;
                }
            }
        }

        return null;
    }

    public List<String> getNamespaces() {
        return Collections.unmodifiableList(namespaces);
    }

    public String getSelectedNamespace() {
        return selectedNamespace;
    }

    public void setSelectedNamespace(String namespace) {
        if (namespaces.contains(namespace)) {
            this.selectedNamespace = namespace;
            notifyDataChanged();
        }
    }

    public boolean hasNamespaces() {
        return !namespaces.isEmpty();
    }

    public void reload() {
        loadNamespacesFromAppSettings();
    }

    public ApiResponse createException(ExceptionItem exceptionItem, String token) {
        // API call to create the exception
        String apiUrl = "https://webapi.tobit.com/chaynserrors/v1/Codes";

        // Build JSON payload
        String jsonBody = String.format(
                "{\"code\":\"%s%s\",\"statusCode\":%d,\"description\":\"%s\",\"logLevel\":%d,\"textGer\":\"%s\"}",
                selectedNamespace,
                exceptionItem.code(),
                exceptionItem.statusCode(),
                exceptionItem.description(),
                exceptionItem.LogLevel(),
                exceptionItem.message()
        );

        return apiService.postRequest(apiUrl, jsonBody,
                Collections.singletonMap("Authorization", "Bearer " + token));
    }
}