package com.wealthsearch.web.error;

import java.util.Locale;

public enum ErrorMessage {
    FIELD_VALIDATION_ERROR("Validation failed for field '%s': %s"),
    GLOBAL_VALIDATION_ERROR("Validation failed: %s"),
    CONSTRAINT_VIOLATION("Constraint violation at '%s': %s"),
    INTERNAL_ERROR("Unexpected error occurred");

    private final String template;

    ErrorMessage(String template) {
        this.template = template;
    }

    public String message() {
        return template;
    }

    public String format(Object... args) {
        return String.format(Locale.ROOT, template, args);
    }
}
