package com.wealthsearch.model.entity;

public enum DocumentSummaryProcessStatus {

    IN_PROGRESS,
    COMPLETED,
    FAILED,
    TIMED_OUT;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == TIMED_OUT;
    }
}
