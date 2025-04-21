package com.tobit.plugin.models.data;

public record ApiResponse(String data, int statusCode) {

    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    public boolean isForbidden() {
        return statusCode == 403;
    }
}