package com.wealthsearch.service.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "document-summary.process")
public class DocumentSummaryProcessProperties {

    /**
     * Длительность, по истечении которой обрабатываемый элемент считается устаревшим и может быть повторен.
     */
    private Duration staleAfter = Duration.ofMinutes(15);

    public Duration getStaleAfter() {
        return staleAfter;
    }

    public void setStaleAfter(Duration staleAfter) {
        this.staleAfter = staleAfter;
    }
}