package com.wealthsearch.model.exception;

import com.wealthsearch.model.error.ErrorEntry;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BadRequestException extends RuntimeException {

    private final List<ErrorEntry> errors;

    public BadRequestException(String message) {
        this(message == null ? List.of() : List.of(new ErrorEntry(message)));
    }

    public BadRequestException(List<ErrorEntry> errors) {
        super(buildMessage(errors));
        this.errors = normalizeErrors(errors);
    }

    public List<ErrorEntry> getErrors() {
        return errors;
    }

    private static String buildMessage(List<ErrorEntry> errors) {
        return normalizeErrors(errors).stream()
                                      .map(ErrorEntry::message)
                                      .collect(Collectors.joining(", "));
    }

    private static List<ErrorEntry> normalizeErrors(List<ErrorEntry> errors) {
        if (errors == null) {
            return List.of();
        }
        return List.copyOf(errors.stream()
                                 .filter(Objects::nonNull)
                                 .collect(Collectors.toList()));
    }
}
