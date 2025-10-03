package com.wealthsearch.client.ollama;

import com.wealthsearch.client.ollama.dto.OllamaEmbedRequest;
import com.wealthsearch.client.ollama.dto.OllamaEmbedResponse;
import com.wealthsearch.client.ollama.dto.OllamaGenerateResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;

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
