package com.wealthsearch.api;

import com.wealthsearch.model.Document;
import java.util.List;
import java.util.UUID;

public interface DocumentService {
    Document createDocument(UUID clientId, CreateDocumentCommand command);

    List<Document> findByClientId(UUID clientId);
}
