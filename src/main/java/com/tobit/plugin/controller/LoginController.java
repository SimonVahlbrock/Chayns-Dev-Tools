// src/main/java/com/tobit/plugin/controller/LoginController.java
package com.tobit.plugin.controller;

import com.intellij.openapi.project.Project;
import com.tobit.plugin.services.TokenService;

public class LoginController {
    private final TokenService tokenService;

    public LoginController(Project project) {
        this.tokenService = TokenService.getInstance(project);
    }

    public boolean getToken() {
        return tokenService.remoteLogin();
    }

    public void addTokenChangeListener(TokenService.TokenChangeListener listener) {
        tokenService.addTokenChangeListener(listener);
    }

    public void removeTokenChangeListener(TokenService.TokenChangeListener listener) {
        tokenService.removeTokenChangeListener(listener);
    }
}