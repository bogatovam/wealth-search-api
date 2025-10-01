package com.wealthsearch.db.repository;

import com.wealthsearch.model.Document;
import java.util.List;
import java.util.UUID;

public interface DocumentRepository {
    Document save(Document document);

    List<Document> findByClientId(UUID clientId);

    List<Document> searchByContent(String searchTerm);
}
