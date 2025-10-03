package com.wealthsearch.db.repository;

import com.wealthsearch.model.entity.Client;
import com.wealthsearch.model.entity.search.ClientSearchHit;
import com.wealthsearch.model.entity.search.PaginationParams;
import com.wealthsearch.model.entity.search.SearchResult;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface ClientRepository {

    Client save(Client client);

    Optional<Client> findById(UUID clientId);

    SearchResult<ClientSearchHit> findClientsByCompanyDomain(List<String> domains, PaginationParams paginationParams);
}
