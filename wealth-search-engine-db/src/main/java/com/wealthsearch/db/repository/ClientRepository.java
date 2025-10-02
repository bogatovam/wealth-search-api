package com.wealthsearch.db.repository;

import com.wealthsearch.model.*;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface ClientRepository {

    Client save(Client client);

    Optional<Client> findById(UUID clientId);

    SearchResult<ClientSearchHit> findClientsByCompanyDomain(List<String> domains, PaginationParams paginationParams);
}
