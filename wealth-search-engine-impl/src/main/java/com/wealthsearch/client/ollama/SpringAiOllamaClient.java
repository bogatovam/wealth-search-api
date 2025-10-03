package com.wealthsearch.client.ollama;

import com.wealthsearch.client.ollama.dto.OllamaEmbedRequest;
import com.wealthsearch.client.ollama.dto.OllamaEmbedResponse;
import com.wealthsearch.client.ollama.dto.OllamaGenerateResponse;
import com.wealthsearch.model.exception.OllamaClientException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

import static com.wealthsearch.client.ollama.OllamaResponseMapper.*;
import static com.wealthsearch.client.ollama.ResilienceConfigurationHelper.*;

@Slf4j
@Component
public class SpringAiOllamaClient implements OllamaClient {

    private final OllamaApi ollamaApi;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public SpringAiOllamaClient(OllamaApi ollamaApi, CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        this.ollamaApi = ollamaApi;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(SpringAiOllamaClient.class.getSimpleName());
        this.retry = retryRegistry.retry(SpringAiOllamaClient.class.getSimpleName());
    }

    @PostConstruct
    public void init() {
        configureCircuitBreakerEvents(circuitBreaker);
        configureRetryEvents(retry);
    }

    @Override
    public OllamaGenerateResponse generate(OllamaApi.ChatRequest request) {
        return executeWithResilience(() -> generateInternal(request), "generate");
    }

    @Override
    public OllamaEmbedResponse embed(OllamaEmbedRequest request) {
        return executeWithResilience(() -> embedInternal(request), "embed");
    }

    private OllamaGenerateResponse generateInternal(OllamaApi.ChatRequest request) {
        try {
            OllamaApi.ChatResponse response = ollamaApi.chat(request);

            return mapGenerateResponse(response);
        } catch (Exception ex) {
            throw new OllamaClientException("Failed to generate response", ex);
        }
    }

    private OllamaEmbedResponse embedInternal(OllamaEmbedRequest request) {
        try {
            OllamaApi.EmbeddingsResponse embedResponse =
                    ollamaApi.embed(new OllamaApi.EmbeddingsRequest(request.getModel(), request.getPrompt()));
            return mapEmbedResponse(embedResponse);
        } catch (Exception ex) {
            throw new OllamaClientException("Failed to generate embeddings", ex);
        }
    }

    private <T> T executeWithResilience(Supplier<T> supplier, String operation) {
        try {

            Supplier<T> decoratedSupplier =
                    CircuitBreaker.decorateSupplier(circuitBreaker, Retry.decorateSupplier(retry, supplier));
            return decoratedSupplier.get();

        } catch (Exception ex) {
            log.error("Ollama {} operation failed after resilience handling: {}", operation, ex.getMessage(), ex);
            throw new OllamaClientException("Failed to execute " + operation + " operation", ex);
        }
    }
}
