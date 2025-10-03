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

        assertThat(firstCall).containsExactlyInAnyOrder("wealth management", "financial planning", "wealth");
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

    @Test
    void expandQueryWithSynonymsFallsBackToOriginalQueryOnException() {
        when(ollamaClient.generate(ArgumentMatchers.any(Prompt.class))).thenThrow(new RuntimeException(
                "Ollama service unavailable"));

        Set<String> terms = expanderService.expandQueryWithSynonyms("wealth");

        assertThat(terms).containsExactly("wealth");
    }

    @Test
    void expandQueryWithSynonymsHandlesEmptyResponse() {
        FtsQueryExpandResult response = new FtsQueryExpandResult();
        response.setSynonyms(List.of());
        response.setRelated(List.of());
        response.setNarrower(List.of());

        when(ollamaClient.generate(ArgumentMatchers.any(Prompt.class))).thenReturn(response);

        Set<String> terms = expanderService.expandQueryWithSynonyms("test");

        // Should still include the original query even when Ollama returns
        // empty
        assertThat(terms).containsExactly("test");
    }

    @Test
    void expandQueryWithSynonymsHandlesNullLists() {
        FtsQueryExpandResult response = new FtsQueryExpandResult();
        response.setSynonyms(null);
        response.setRelated(null);
        response.setNarrower(null);

        when(ollamaClient.generate(ArgumentMatchers.any(Prompt.class))).thenReturn(response);

        Set<String> terms = expanderService.expandQueryWithSynonyms("test");

        // Should handle gracefully - the original query is added after
        // collecting
        assertThat(terms).containsExactly("test");
    }

    @Test
    void expandQueryWithSynonymsIgnoresBlankTerms() {
        FtsQueryExpandResult response = new FtsQueryExpandResult();
        response.setSynonyms(List.of("valid", "", "  "));
        response.setRelated(List.of("also-valid", "   "));
        response.setNarrower(List.of());

        when(ollamaClient.generate(ArgumentMatchers.any(Prompt.class))).thenReturn(response);

        Set<String> terms = expanderService.expandQueryWithSynonyms("test");

        assertThat(terms).containsExactlyInAnyOrder("valid", "also-valid", "test");
    }

    @Test
    void expandQueryWithSynonymsHandlesDuplicateTerms() {
        FtsQueryExpandResult response = new FtsQueryExpandResult();
        response.setSynonyms(List.of("wealth management", "financial planning"));
        response.setRelated(List.of("financial planning", "investment"));
        response.setNarrower(List.of("wealth management"));

        when(ollamaClient.generate(ArgumentMatchers.any(Prompt.class))).thenReturn(response);

        Set<String> terms = expanderService.expandQueryWithSynonyms("wealth");

        // Set should deduplicate and include original query
        assertThat(terms).containsExactlyInAnyOrder("wealth management", "financial planning", "investment", "wealth");
    }

    @Test
    void expandQueryWithSynonymsCachesMultipleDifferentQueries() {
        FtsQueryExpandResult response1 = new FtsQueryExpandResult();
        response1.setSynonyms(List.of("wealth management"));
        response1.setRelated(List.of());
        response1.setNarrower(List.of());

        FtsQueryExpandResult response2 = new FtsQueryExpandResult();
        response2.setSynonyms(List.of("financial planning"));
        response2.setRelated(List.of());
        response2.setNarrower(List.of());

        when(ollamaClient.generate(ArgumentMatchers.any(Prompt.class))).thenReturn(response1)
                                                                       .thenReturn(response2);

        Set<String> result1 = expanderService.expandQueryWithSynonyms("wealth");
        Set<String> result2 = expanderService.expandQueryWithSynonyms("finance");
        Set<String> result3 = expanderService.expandQueryWithSynonyms("wealth"); // cached

        assertThat(result1).containsExactlyInAnyOrder("wealth management", "wealth");
        assertThat(result2).containsExactlyInAnyOrder("financial planning", "finance");
        assertThat(result3).isEqualTo(result1);

        verify(ollamaClient, times(2)).generate(ArgumentMatchers.any(Prompt.class));
    }

    @Test
    void expandQueryWithSynonymsHandlesLargeResponse() {
        List<String> manySynonyms = List.of("term1", "term2", "term3", "term4", "term5");
        List<String> manyRelated = List.of("rel1", "rel2", "rel3", "rel4", "rel5");
        List<String> manyNarrower = List.of("nar1", "nar2", "nar3", "nar4", "nar5");

        FtsQueryExpandResult response = new FtsQueryExpandResult();
        response.setSynonyms(manySynonyms);
        response.setRelated(manyRelated);
        response.setNarrower(manyNarrower);

        when(ollamaClient.generate(ArgumentMatchers.any(Prompt.class))).thenReturn(response);

        Set<String> terms = expanderService.expandQueryWithSynonyms("test");

        // 15 terms from Ollama + 1 original query
        assertThat(terms).hasSize(16);
    }

    @Test
    void promptTemplateIsLoadedCorrectly() {
        // This is implicitly tested by setUp, but let's verify the service is
        // usable
        FtsQueryExpandResult response = new FtsQueryExpandResult();
        response.setSynonyms(List.of("test"));
        response.setRelated(List.of());
        response.setNarrower(List.of());

        when(ollamaClient.generate(ArgumentMatchers.any(Prompt.class))).thenReturn(response);

        Set<String> terms = expanderService.expandQueryWithSynonyms("query");

        assertThat(terms).isNotNull();
        verify(ollamaClient).generate(ArgumentMatchers.any(Prompt.class));
    }

    @Test
    void expandQueryIncludesOriginalQueryInResponse() {
        FtsQueryExpandResult response = new FtsQueryExpandResult();
        response.setSynonyms(List.of("synonym1", "synonym2"));
        response.setRelated(List.of());
        response.setNarrower(List.of());

        when(ollamaClient.generate(ArgumentMatchers.any(Prompt.class))).thenReturn(response);

        Set<String> terms = expanderService.expandQueryWithSynonyms("original");

        // Original query should be included
        assertThat(terms).contains("original");
    }
}
