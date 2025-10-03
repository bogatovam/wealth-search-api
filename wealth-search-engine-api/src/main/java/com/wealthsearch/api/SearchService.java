package com.wealthsearch.api;

import com.wealthsearch.model.entity.search.ClientSearchHit;
import com.wealthsearch.model.entity.search.DocumentSearchHit;
import com.wealthsearch.model.entity.search.PaginationParams;
import com.wealthsearch.model.entity.search.SearchResult;

public interface SearchService {

    SearchResult<ClientSearchHit> searchClientsPerCompanyName(String query, PaginationParams paginationParams);

    SearchResult<DocumentSearchHit> searchDocumentsBySimilarTerms(String query, PaginationParams paginationParams);
}
