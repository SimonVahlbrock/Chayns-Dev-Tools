package com.tobit.plugin.models;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.tobit.plugin.models.data.ExceptionItem;
import com.tobit.plugin.services.ApiService;
import com.tobit.plugin.models.data.ApiResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChaynsExceptionModel {
    private final Project project;
    private String namespace = "";
    private final List<ExceptionItem> exceptionTypes = new ArrayList<>();
    private final ApiService apiService = new ApiService();

    private final List<DataChangeListener> dataChangeListeners = new ArrayList<>();

    public interface DataChangeListener {
        void onDataChanged(String namespace, List<ExceptionItem> exceptionTypes);
    }

    public ChaynsExceptionModel(Project project) {
        this.project = project;
        loadNamespaceFromAppSettings();
        loadExceptionsFromApi();
    }

    public void addDataChangeListener(DataChangeListener listener) {
        dataChangeListeners.add(listener);
    }

    public void removeDataChangeListener(DataChangeListener listener) {
        dataChangeListeners.remove(listener);
    }

    private void notifyDataChanged() {
        for (DataChangeListener listener : dataChangeListeners) {
            listener.onDataChanged(namespace, exceptionTypes);
        }
    }

    private void loadNamespaceFromAppSettings() {
        VirtualFile appSettingsFile = findFileInProject("appsettings.json");
        if (appSettingsFile != null) {
            try {
                String content = new String(appSettingsFile.contentsToByteArray(), StandardCharsets.UTF_8);
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(content, JsonObject.class);

                if (jsonObject.has("ChaynsErrors") &&
                        jsonObject.getAsJsonObject("ChaynsErrors").has("Namespaces")) {
                    String firstNamespace = jsonObject.getAsJsonObject("ChaynsErrors")
                            .getAsJsonArray("Namespaces")
                            .get(0)
                            .getAsString();
                    this.namespace = firstNamespace;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadExceptionsFromApi() {
        if (namespace.isEmpty()) {
            return;
        }

        exceptionTypes.clear();

        String apiUrl = "https://webapi.tobit.com/chaynserrors/v1/Codes?filter=" + namespace + "&withAllTranslations=false";
        ApiResponse response = apiService.getRequest(apiUrl);

        if (response.statusCode() == 200) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<ApiExceptionItem>>(){}.getType();
            List<ApiExceptionItem> apiExceptions = gson.fromJson(response.data(), listType);

            for (ApiExceptionItem apiException : apiExceptions) {
                exceptionTypes.add(new ExceptionItem(
                        apiException.code,
                        apiException.description,
                        apiException.statusCode
                ));
            }
        }

        notifyDataChanged();
    }

    // Private class to parse API response
    private static class ApiExceptionItem {
        String code;
        String description;
        int statusCode;
        // Other fields from API response not used
    }

    /**
     * Recursively searches for a file with the given name in the project
     *
     * @param fileName The name of the file to search for
     * @return The found VirtualFile or null if not found
     */
    private VirtualFile findFileInProject(String fileName) {
        VirtualFile baseDir = project.getBaseDir();
        return findFileRecursively(baseDir, fileName);
    }

    /**
     * Recursively searches for a file in the given directory and its subdirectories
     *
     * @param directory The directory to search in
     * @param fileName The name of the file to search for
     * @return The found VirtualFile or null if not found
     */
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

    public String getNamespace() {
        return namespace;
    }

    public List<ExceptionItem> getExceptionTypes() {
        return Collections.unmodifiableList(exceptionTypes);
    }

    public void reload() {
        loadNamespaceFromAppSettings();
        loadExceptionsFromApi();
    }
}