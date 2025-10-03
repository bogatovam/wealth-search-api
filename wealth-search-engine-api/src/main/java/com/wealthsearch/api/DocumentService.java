package com.wealthsearch.api;

import com.wealthsearch.model.Document;
import com.wealthsearch.model.DocumentSummaryProcessItem;

import java.util.List;
import java.util.UUID;

public interface DocumentService {
    Document createDocument(Document document);

    DocumentSummaryProcessItem generateSummaryForDocument(UUID clientId, UUID documentId);
}
