// src/main/java/com/tobit/plugin/service/StorageService.java
package com.tobit.plugin.services;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

public class StorageService {
    private final PropertiesComponent properties;

    public StorageService(Project project) {
        this.properties = PropertiesComponent.getInstance(project);
    }

    public void setValue(String key, String value) {
        properties.setValue(key, value);
    }

    @Nullable
    public String getValue(String key) {
        return properties.getValue(key);
    }

    public void setValue(String key, int value) {
        properties.setValue(key, Integer.toString(value));
    }

    public int getIntValue(String key, int defaultValue) {
        String value = properties.getValue(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                // Fall through to return default value
            }
        }
        return defaultValue;
    }
}