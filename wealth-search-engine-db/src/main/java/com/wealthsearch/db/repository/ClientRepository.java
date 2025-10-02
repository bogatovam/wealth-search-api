package com.wealthsearch.db.repository;

import com.wealthsearch.model.Client;
import com.wealthsearch.model.ClientSearchHit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository {

    Client save(Client client);

    Optional<Client> findById(UUID clientId);

    List<ClientSearchHit> findClientsByCompanyDomain(List<String> domains);
}
