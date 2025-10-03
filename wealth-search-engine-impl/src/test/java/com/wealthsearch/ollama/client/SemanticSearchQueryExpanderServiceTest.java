package com.wealthsearch.ollama.client;

import com.wealthsearch.api.ollama.client.OllamaClient;
import com.wealthsearch.model.ollama.FtsQueryExpandResult;
import com.wealthsearch.ollama.SemanticSearchQueryExpanderService;
import com.wealthsearch.ollama.client.confiuration.OllamaChatRequestProperties;
import org.springframework.ai.chat.prompt.Prompt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticSearchQueryExpanderServiceTest {

    @Mock
    private OllamaClient ollamaClient;

    private SemanticSearchQueryExpanderService expanderService;

    @BeforeEach
    void setUp() {
        OllamaChatRequestProperties properties = new OllamaChatRequestProperties();
        properties.setFormat("json");

        expanderService = new SemanticSearchQueryExpanderService(ollamaClient, properties);

        ReflectionTestUtils.setField(expanderService, "synonymPromptResource",
                new ByteArrayResource("Query: \"{QUERY}\"".getBytes(StandardCharsets.UTF_8)));
        ReflectionTestUtils.setField(expanderService, "chatModel", "test-model");
        ReflectionTestUtils.setField(expanderService, "cacheMaximumSize", 16);
        ReflectionTestUtils.setField(expanderService, "cacheExpireAfterMinutes", 30L);

        expanderService.loadResources();
    }

    @Test
    void expandQueryWithSynonymsCachesRemoteResponses() {
        FtsQueryExpandResult response = new FtsQueryExpandResult();
        response.setSynonyms(List.of("wealth management"));
        response.setRelated(List.of("financial planning"));
        response.setNarrower(List.of());

        when(ollamaClient.generate(ArgumentMatchers.any(Prompt.class))).thenReturn(response);

        Set<String> firstCall = expanderService.expandQueryWithSynonyms("wealth");
        Set<String> secondCall = expanderService.expandQueryWithSynonyms("wealth");

        assertThat(firstCall).containsExactlyInAnyOrder("wealth management", "financial planning");
        assertThat(secondCall).isEqualTo(firstCall);

        verify(ollamaClient, times(1)).generate(ArgumentMatchers.any(Prompt.class));
    }

    @Test
    void expandQueryWithSynonymsCollectsAllCategories() {
        FtsQueryExpandResult response = new FtsQueryExpandResult();
        response.setSynonyms(List.of("wealth management"));
        response.setRelated(List.of("financial planning"));
        response.setNarrower(List.of("portfolio review"));

        when(ollamaClient.generate(ArgumentMatchers.any(Prompt.class))).thenReturn(response);

        Set<String> terms = expanderService.expandQueryWithSynonyms("wealth");

        assertThat(terms).containsExactlyInAnyOrder("wealth management", "financial planning", "portfolio review");
        verify(ollamaClient, times(1)).generate(ArgumentMatchers.any(Prompt.class));
    }
}
