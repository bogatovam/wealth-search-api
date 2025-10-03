package com.wealthsearch.client.ollama;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ResilienceConfigurationHelper {

    private ResilienceConfigurationHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void configureCircuitBreakerEvents(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
                      .onStateTransition(event -> log.warn("Ollama circuit breaker state changed: {}", event))
                      .onError(event -> log.error("Ollama circuit breaker error: {}", event.getThrowable()
                                                                                           .getMessage()))
                      .onSuccess(event -> log.debug("Ollama circuit breaker success"))
                      .onCallNotPermitted(event -> log.error("Ollama circuit breaker is OPEN - calls not permitted"));
    }

    public static void configureRetryEvents(Retry retry) {
        retry.getEventPublisher()
             .onRetry(event -> log.warn("Ollama retry attempt #{}: {}", event.getNumberOfRetryAttempts(),
                                        event.getLastThrowable()
                                             .getMessage()));
    }
}
