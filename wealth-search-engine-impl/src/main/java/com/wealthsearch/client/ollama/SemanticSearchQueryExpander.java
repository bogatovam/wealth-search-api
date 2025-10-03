package com.wealthsearch.client.ollama;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wealthsearch.client.ollama.dto.OllamaGenerateResponse;
import com.wealthsearch.service.util.SearchQueryUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticSearchQueryExpander {

    private final OllamaClient ollamaClient;

    private final OllamaChatRequestProperties chatRequestProperties;

    @Value("${semantic-search.prompts.synonym-path}")
    private Resource synonymPromptResource;

    @Value("${spring.ai.ollama.chat.model}")
    private String chatModel;

    private PromptTemplate promptTemplate;

    @PostConstruct
    void loadPromptTemplate() {
        try (InputStreamReader reader =
                new InputStreamReader(synonymPromptResource.getInputStream(), StandardCharsets.UTF_8)) {
            this.promptTemplate = new PromptTemplate(FileCopyUtils.copyToString(reader));

        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load synonym expansion prompt", ex);
        }
    }

    public Set<String> expandQueryWithSynonyms(String query) {
        OllamaOptions options = this.buildOllamaRequestWithQuery();
        Prompt prompt = promptTemplate.create(Map.of("QUERY", query), options);

        FtsQueryExpandResult response = ollamaClient.generate(prompt);

        return this.collectTermsFromOllamaResponse(response);
    }

    private OllamaOptions buildOllamaRequestWithQuery() {
        return chatRequestProperties.optionsAsBuilder()
                                    .model(chatModel)
                                    .format(chatRequestProperties.getFormat())
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
