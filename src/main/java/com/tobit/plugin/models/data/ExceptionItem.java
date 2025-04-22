package com.tobit.plugin.models.data;

public record ExceptionItem(
        String code,
        String description,
        int statusCode,
        Integer LogLevel,
        String message
) {
    public ExceptionItem(String code, String description, int statusCode) {
        this(code, description, statusCode, null, null);
    }

    public String getName() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}