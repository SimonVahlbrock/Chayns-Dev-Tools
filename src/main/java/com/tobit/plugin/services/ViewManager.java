package com.tobit.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;

import java.util.HashMap;
import java.util.Map;

@Service(Service.Level.PROJECT)
public final class ViewManager {
    private final Project project;
    private final Map<Class<?>, Object> viewRegistry = new HashMap<>();

    public ViewManager(Project project) {
        this.project = project;
    }

    public <T> void registerView(Class<T> viewClass, T view) {
        viewRegistry.put(viewClass, view);
    }

    @SuppressWarnings("unchecked")
    public <T> T getView(Class<T> viewClass) {
        return (T) viewRegistry.get(viewClass);
    }

    public static ViewManager getInstance(Project project) {
        return ServiceManager.getService(project, ViewManager.class);
    }
}