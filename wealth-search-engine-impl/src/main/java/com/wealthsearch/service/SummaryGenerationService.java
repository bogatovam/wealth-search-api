package com.wealthsearch.service;

import com.wealthsearch.api.ollama.client.OllamaClient;
import com.wealthsearch.db.repository.DocumentRepository;
import com.wealthsearch.db.repository.DocumentSummaryProcessItemRepository;
import com.wealthsearch.model.entity.Document;
import com.wealthsearch.model.entity.DocumentSummaryProcessItem;
import com.wealthsearch.model.entity.DocumentSummaryProcessStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SummaryGenerationService {

    private final OllamaClient ollamaClient;

    @Value("${document-summary.prompts.short-summary}")
    private Resource synonymPromptResource;

    private PromptTemplate promptTemplate;

    private DocumentSummaryProcessItemRepository summaryProcessItemRepository;

    @Transactional
    public void generateSummaryForDocumentAsync(Document document,
            DocumentSummaryProcessItem documentSummaryProcessItem) {

        String summary = ollamaClient.generateAsText(promptTemplate.create(Map.of("INPUT", document.getContent())));

        summaryProcessItemRepository.complete(document.getId(), summary);
    }

    private void loadPromptTemplate() {
        try (InputStreamReader reader =
                new InputStreamReader(synonymPromptResource.getInputStream(), StandardCharsets.UTF_8)) {
            this.promptTemplate = new PromptTemplate(FileCopyUtils.copyToString(reader));

        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load synonym expansion prompt", ex);
        }
    }
}
