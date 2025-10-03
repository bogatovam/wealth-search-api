package com.wealthsearch.model.entity;

public enum DocumentSummaryProcessStatus {

    IN_PROGRESS,
    FAILED,
    COMPLETED;

    public boolean isTerminal() {
        return this == COMPLETED;
    }
}
