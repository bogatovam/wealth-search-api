package com.wealthsearch.api;

import com.wealthsearch.model.ClientSearchHit;
import com.wealthsearch.model.DocumentSearchHit;
import com.wealthsearch.model.PaginationParams;
import com.wealthsearch.model.SearchResult;

public interface SearchService {

    SearchResult<ClientSearchHit> searchClientsPerCompanyName(String query, PaginationParams paginationParams);

    SearchResult<DocumentSearchHit> searchDocumentsBySimilarTerms(String query, PaginationParams paginationParams);
}
