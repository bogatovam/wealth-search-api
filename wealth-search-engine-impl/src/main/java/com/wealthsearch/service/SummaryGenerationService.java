package com.wealthsearch.service;

import com.wealthsearch.api.ollama.client.OllamaClient;
import com.wealthsearch.db.repository.DocumentRepository;
import com.wealthsearch.db.repository.DocumentSummaryProcessItemRepository;
import com.wealthsearch.model.entity.Document;
import com.wealthsearch.model.entity.DocumentSummaryProcessItem;
import com.wealthsearch.model.entity.DocumentSummaryProcessStatus;
import com.wealthsearch.model.ollama.SummaryResult;
import com.wealthsearch.ollama.client.confiuration.OllamaChatRequestProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryGenerationService {

    @Value("${spring.ai.ollama.chat.model}")
    private String chatModel;

    @Value("${document-summary.prompts.short-summary}")
    private Resource synonymPromptResource;

    private PromptTemplate promptTemplate;

    private final DocumentSummaryProcessItemRepository summaryProcessItemRepository;

    private final OllamaChatRequestProperties chatRequestProperties;

    private final OllamaClient ollamaClient;

    @PostConstruct
    private void loadPromptTemplate() {
        try (InputStreamReader reader =
                new InputStreamReader(synonymPromptResource.getInputStream(), StandardCharsets.UTF_8)) {
            this.promptTemplate = new PromptTemplate(FileCopyUtils.copyToString(reader));

        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load synonym expansion prompt", ex);
        }
    }

    @Transactional
    public void generateSummaryForDocumentAsync(Document document,
            DocumentSummaryProcessItem documentSummaryProcessItem) {
        log.info("Start generation summary for document {}", document.getId());
        OllamaOptions options = this.buildOllamaRequestWithQuery();

        SummaryResult summary =
                ollamaClient.generateSummary(promptTemplate.create(Map.of("INPUT", document.getContent()), options));

        log.info("Ollama summary response: {}", summary);

        if (StringUtils.isEmpty(summary.getSummary())) {
            summaryProcessItemRepository.markStatus(documentSummaryProcessItem.getDocumentId(),
                                                    DocumentSummaryProcessStatus.FAILED);
        } else {
            summaryProcessItemRepository.complete(document.getId(), summary.getSummary());
        }
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
}
