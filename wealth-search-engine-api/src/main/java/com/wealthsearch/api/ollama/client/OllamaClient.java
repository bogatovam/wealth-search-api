package com.wealthsearch.api.ollama.client;

import com.wealthsearch.model.ollama.FtsQueryExpandResult;
import com.wealthsearch.model.ollama.OllamaEmbedRequest;
import com.wealthsearch.model.ollama.OllamaEmbedResponse;
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

    /**
     * Generate embeddings using Ollama.
     *
     * @param request the embedding request
     * @return the embedding response
     */
    OllamaEmbedResponse embed(OllamaEmbedRequest request);
}
