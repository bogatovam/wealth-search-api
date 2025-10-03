package com.wealthsearch.ollama.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wealthsearch.api.ollama.client.OllamaClient;
import com.wealthsearch.model.ollama.FtsQueryExpandResult;
import com.wealthsearch.model.exception.OllamaClientException;
import com.wealthsearch.model.ollama.SummaryResult;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

import static com.wealthsearch.utils.ResilienceConfigurationHelper.*;

@Slf4j
@Component
public class SpringAiOllamaClient implements OllamaClient {

    private final CircuitBreaker circuitBreaker;

    private final ChatClient chatClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SpringAiOllamaClient(CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry,
            ChatClient.Builder chatClientBuilder) {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(SpringAiOllamaClient.class.getSimpleName());
        this.chatClient = chatClientBuilder.defaultAdvisors(new SimpleLoggerAdvisor())
                                           .build();
    }

    @PostConstruct
    public void init() {
        configureCircuitBreakerEvents(circuitBreaker);
    }

    @Override
    public FtsQueryExpandResult generate(Prompt prompt) {
        return executeWithResilience(() -> generateInternal(prompt, FtsQueryExpandResult.class), "generate");
    }

    @Override
    public SummaryResult generateSummary(Prompt prompt) {
        return executeWithResilience(() -> generateInternal(prompt, SummaryResult.class), "generate");
    }

    private <T> T generateInternal(Prompt prompt, Class<T> clazz) {
        try {
            String content = this.chatClient.prompt(prompt)
                                            .call()
                                            .content();
            return objectMapper.readValue(content, clazz);
        } catch (Exception ex) {
            throw new OllamaClientException("Failed to generate response", ex);
        }
    }

    private <T> T executeWithResilience(Supplier<T> supplier, String operation) {
        try {

            Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
            return decoratedSupplier.get();

        } catch (Exception ex) {
            log.error("Ollama {} operation failed after resilience handling: {}", operation, ex.getMessage(), ex);
            throw new OllamaClientException("Failed to execute " + operation + " operation", ex);
        }
    }
}
