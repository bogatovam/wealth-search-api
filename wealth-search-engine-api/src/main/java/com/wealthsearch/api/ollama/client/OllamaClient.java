package com.wealthsearch.api.ollama.client;

import com.wealthsearch.model.ollama.FtsQueryExpandResult;
import com.wealthsearch.model.ollama.SummaryResult;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Client interface for interacting with Ollama API.
 */
public interface OllamaClient {

    /**
     * Generate text completion using Ollama.
     *
     * @param prompt
     * @return the generation response
     */
    FtsQueryExpandResult generate(Prompt prompt);

    SummaryResult generateSummary(Prompt input);
}
