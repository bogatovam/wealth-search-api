package com.wealthsearch.client.ollama;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
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
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SpringAiOllamaClient(OllamaApi ollamaApi, CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry, ChatClient.Builder chatClientBuilder) {
        this.ollamaApi = ollamaApi;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(SpringAiOllamaClient.class.getSimpleName());
        this.retry = retryRegistry.retry(SpringAiOllamaClient.class.getSimpleName());

        this.chatClient = chatClientBuilder.defaultAdvisors(new SimpleLoggerAdvisor())
                                           .build();
    }

    @PostConstruct
    public void init() {
        configureCircuitBreakerEvents(circuitBreaker);
        configureRetryEvents(retry);
    }

    @Override
    public FtsQueryExpandResult generate(Prompt prompt) {
        return executeWithResilience(() -> generateInternal(prompt), "generate");
    }

    @Override
    public OllamaEmbedResponse embed(OllamaEmbedRequest request) {
        return executeWithResilience(() -> embedInternal(request), "embed");
    }

    private FtsQueryExpandResult generateInternal(Prompt prompt) {
        try {
            String content = this.chatClient.prompt(prompt)
                                            .call()
                                            .content();
            return objectMapper.readValue(content, FtsQueryExpandResult.class);
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
