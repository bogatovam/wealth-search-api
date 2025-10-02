package com.wealthsearch.client.ollama;

import com.wealthsearch.client.ollama.dto.OllamaEmbedRequest;
import com.wealthsearch.client.ollama.dto.OllamaEmbedResponse;
import com.wealthsearch.client.ollama.dto.OllamaGenerateRequest;
import com.wealthsearch.client.ollama.dto.OllamaGenerateResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Resilient implementation of OllamaClient using WebClient with circuit breaker, retry, and timeout.
 * This implementation protects against failures in the unreliable Ollama service.
 */
@Slf4j
@Component
public class ResilientOllamaWebClient implements OllamaClient {

    private static final String CIRCUIT_BREAKER_NAME = "ollama";
    private static final String RETRY_NAME = "ollama";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public ResilientOllamaWebClient(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
        this.retry = retryRegistry.retry(RETRY_NAME);

        // Log circuit breaker state changes
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.warn("Ollama circuit breaker state changed: {}", event))
                .onError(event -> log.error("Ollama circuit breaker error: {}", event.getThrowable().getMessage()))
                .onSuccess(event -> log.debug("Ollama circuit breaker success"))
                .onCallNotPermitted(event -> log.error("Ollama circuit breaker is OPEN - calls not permitted"));

        // Log retry events
        retry.getEventPublisher()
                .onRetry(event -> log.warn("Ollama retry attempt #{}: {}",
                        event.getNumberOfRetryAttempts(),
                        event.getLastThrowable().getMessage()));
    }

    @Override
    public OllamaGenerateResponse generate(OllamaGenerateRequest request) {
        log.debug("Generating text with model: {}", request.getModel());

        Supplier<OllamaGenerateResponse> supplier = () ->
                executeRequest("/api/generate", request, OllamaGenerateResponse.class);

        return executeWithResilience(supplier, "generate");
    }

    @Override
    public OllamaEmbedResponse embed(OllamaEmbedRequest request) {
        log.debug("Generating embeddings with model: {}", request.getModel());

        Supplier<OllamaEmbedResponse> supplier = () ->
                executeRequest("/api/embeddings", request, OllamaEmbedResponse.class);

        return executeWithResilience(supplier, "embed");
    }

    @Override
    public boolean isHealthy() {
        try {
            webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(5));
            return true;
        } catch (Exception e) {
            log.warn("Ollama health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Execute request with all resilience patterns applied.
     */
    private <T> T executeWithResilience(Supplier<T> supplier, String operation) {
        try {
            // Apply resilience patterns: Retry -> CircuitBreaker
            Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(
                    circuitBreaker,
                    Retry.decorateSupplier(retry, supplier)
            );

            return decoratedSupplier.get();
        } catch (Exception e) {
            log.error("Ollama {} operation failed after all resilience attempts: {}",
                    operation, e.getMessage(), e);
            throw new OllamaClientException("Failed to execute " + operation + " operation", e);
        }
    }

    /**
     * Execute HTTP request using WebClient with timeout.
     */
    private <T> T executeRequest(String path, Object request, Class<T> responseType) {
        try {
            return webClient.post()
                    .uri(path)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(responseType)
                    .timeout(REQUEST_TIMEOUT)
                    .onErrorMap(WebClientResponseException.class, e -> {
                        log.error("Ollama API error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
                        return new OllamaClientException("Ollama API error: " + e.getMessage(), e);
                    })
                    .onErrorMap(e -> !(e instanceof OllamaClientException), e -> {
                        log.error("Ollama connection error: {}", e.getMessage());
                        return new OllamaClientException("Ollama connection failed", e);
                    })
                    .block();
        } catch (Exception e) {
            throw new OllamaClientException("Request execution failed", e);
        }
    }

    /**
     * Custom exception for Ollama client errors.
     */
    public static class OllamaClientException extends RuntimeException {
        public OllamaClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
