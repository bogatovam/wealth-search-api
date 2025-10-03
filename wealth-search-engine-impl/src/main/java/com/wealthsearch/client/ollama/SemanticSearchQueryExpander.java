package com.wealthsearch.client.ollama;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wealthsearch.client.ollama.dto.OllamaGenerateResponse;
import com.wealthsearch.service.util.SearchQueryUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticSearchQueryExpander {

    private final OllamaClient ollamaClient;

    private final ObjectMapper objectMapper;

    private final OllamaChatRequestProperties chatRequestProperties;

    @Value("${semantic-search.prompts.synonym-path}")
    private Resource synonymPromptResource;

    @Value("${spring.ai.ollama.chat.model}")
    private String chatModel;

    private String promptTemplate;

    @PostConstruct
    void loadPromptTemplate() {
        try (InputStreamReader reader =
                new InputStreamReader(synonymPromptResource.getInputStream(), StandardCharsets.UTF_8)) {
            this.promptTemplate = FileCopyUtils.copyToString(reader);

        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load synonym expansion prompt", ex);
        }
    }

    public Set<String> expandQueryWithSynonyms(String query) {
        OllamaApi.ChatRequest request = this.buildOllamaRequestWithQuery(query);

        OllamaGenerateResponse response = ollamaClient.generate(request);
        try {
            log.info("{}", objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String content = response != null ? response.getResponse() : null;

        if (!StringUtils.hasText(content)) {
            log.warn("Empty response received from Ollama for query expansion");
            return Set.of(query);
        }

        FtsQueryExpandResult result;
        try {
            result = objectMapper.readValue(content, FtsQueryExpandResult.class);
        } catch (IOException ex) {
            log.warn("Failed to parse Ollama query expansion response: {}", ex.getMessage());
            return Set.of(query);
        }

        return this.collectTermsFromOllamaResponse(result);
    }

    private OllamaApi.ChatRequest buildOllamaRequestWithQuery(String query) {
        String prompt = promptTemplate.replace("{{QUERY}}", query);

        return OllamaApi.ChatRequest.builder(chatModel)
                                    .format(chatRequestProperties.getFormat())
                                    .messages(List.of(OllamaApi.Message.builder(OllamaApi.Message.Role.TOOL)
                                                                       .content(prompt)
                                                                       .build()))
                                    .stream(chatRequestProperties.isStream())
                                    .options(chatRequestProperties.copyOptions())
                                    .build();
    }

    private Set<String> collectTermsFromOllamaResponse(FtsQueryExpandResult result) {
        Set<String> terms = new LinkedHashSet<>();
        SearchQueryUtils.collectTerms(result.getSynonyms(), terms);
        SearchQueryUtils.collectTerms(result.getNarrower(), terms);
        SearchQueryUtils.collectTerms(result.getRelated(), terms);

        return Set.copyOf(terms);
    }
}
