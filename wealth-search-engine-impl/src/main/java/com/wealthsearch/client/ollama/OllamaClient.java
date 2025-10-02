package com.wealthsearch.client.ollama;

import com.wealthsearch.client.ollama.dto.OllamaEmbedRequest;
import com.wealthsearch.client.ollama.dto.OllamaEmbedResponse;
import com.wealthsearch.client.ollama.dto.OllamaGenerateRequest;
import com.wealthsearch.client.ollama.dto.OllamaGenerateResponse;

/**
 * Client interface for interacting with Ollama API.
 */
public interface OllamaClient {

    /**
     * Generate text completion using Ollama.
     *
     * @param request the generation request
     * @return the generation response
     */
    OllamaGenerateResponse generate(OllamaGenerateRequest request);

    /**
     * Generate embeddings using Ollama.
     *
     * @param request the embedding request
     * @return the embedding response
     */
    OllamaEmbedResponse embed(OllamaEmbedRequest request);

    /**
     * Check if Ollama service is available.
     *
     * @return true if service is available
     */
    boolean isHealthy();
}
