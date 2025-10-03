package com.wealthsearch.api;

import com.wealthsearch.model.entity.Document;
import com.wealthsearch.model.entity.DocumentSummaryProcessItem;

import java.util.UUID;

public interface DocumentService {
    Document createDocument(Document document);

    DocumentSummaryProcessItem generateSummaryForDocument(UUID documentId);
}
