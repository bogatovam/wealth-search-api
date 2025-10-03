package com.wealthsearch.ollama;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wealthsearch.api.ollama.SemanticSearchQueryExpander;
import com.wealthsearch.api.ollama.client.OllamaClient;
import com.wealthsearch.model.ollama.FtsQueryExpandResult;
import com.wealthsearch.ollama.client.confiuration.OllamaChatRequestProperties;
import com.wealthsearch.utils.SearchQueryUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticSearchQueryExpanderService implements SemanticSearchQueryExpander {

    private final OllamaClient ollamaClient;

    private final OllamaChatRequestProperties chatRequestProperties;

    @Value("${semantic-search.prompts.synonym-path}")
    private Resource synonymPromptResource;

    @Value("${spring.ai.ollama.chat.model}")
    private String chatModel;

    @Value("${semantic-search.synonym-cache.maximum-size:256}")
    private int cacheMaximumSize;

    @Value("${semantic-search.synonym-cache.expire-after-minutes:10}")
    private long cacheExpireAfterMinutes;

    private PromptTemplate promptTemplate;

    private Cache<String, Set<String>> expansionCache;

    @PostConstruct
    public void loadResources() {
        loadPromptTemplate();
        initializeCache();
    }

    @Override
    public Set<String> expandQueryWithSynonyms(String query) {
        try {
            return expansionCache.get(query, this::fetchSynonymExpansions);
        } catch (Exception e) {
            return Set.of(query);
        }
    }

    private Set<String> fetchSynonymExpansions(String query) {
        OllamaOptions options = this.buildOllamaRequestWithQuery();
        Prompt prompt = promptTemplate.create(Map.of("QUERY", query), options);

        FtsQueryExpandResult response = ollamaClient.generate(prompt);

        log.info("Ollama generation response {}", response);
        return this.collectTermsFromOllamaResponse(response, query);
    }

    private OllamaOptions buildOllamaRequestWithQuery() {
        OllamaOptions.Builder builder = chatRequestProperties.optionsAsBuilder();

        if (builder == null) {
            builder = OllamaOptions.builder();
        }

        return builder.model(chatModel)
                      .format(chatRequestProperties.getFormat())
                      .build();
    }

    private Set<String> collectTermsFromOllamaResponse(FtsQueryExpandResult result, String query) {
        Set<String> terms = new LinkedHashSet<>();
        SearchQueryUtils.collectTerms(result.getSynonyms(), terms);
        SearchQueryUtils.collectTerms(result.getNarrower(), terms);
        SearchQueryUtils.collectTerms(result.getRelated(), terms);
        terms.add(query);

        return Set.copyOf(terms);
    }

    private void loadPromptTemplate() {
        try (InputStreamReader reader =
                new InputStreamReader(synonymPromptResource.getInputStream(), StandardCharsets.UTF_8)) {
            this.promptTemplate = new PromptTemplate(FileCopyUtils.copyToString(reader));

        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load synonym expansion prompt", ex);
        }
    }

    private void initializeCache() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                                                   .maximumSize(Math.max(1, cacheMaximumSize));

        if (cacheExpireAfterMinutes > 0) {
            builder = builder.expireAfterWrite(Duration.ofMinutes(cacheExpireAfterMinutes));
        }

        this.expansionCache = builder.build();
    }
}
