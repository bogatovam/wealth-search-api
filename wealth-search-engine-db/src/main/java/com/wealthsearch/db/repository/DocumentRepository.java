package com.wealthsearch.db.repository;

import com.wealthsearch.model.entity.Document;
import com.wealthsearch.model.entity.search.DocumentSearchHit;
import com.wealthsearch.model.entity.search.PaginationParams;
import com.wealthsearch.model.entity.search.SearchResult;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface DocumentRepository {

    Document save(Document document);

    Optional<Document> findById(UUID documentId);

    List<Document> findByClientId(UUID clientId);

    SearchResult<DocumentSearchHit> searchByContent(Set<String> searchTerms, PaginationParams pagination);
}
