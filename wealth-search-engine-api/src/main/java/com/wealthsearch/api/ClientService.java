package com.wealthsearch.api;

import com.wealthsearch.model.entity.Client;
import java.util.Optional;
import java.util.UUID;

public interface ClientService {

    Client createClient(Client client);

    Optional<Client> findById(UUID clientId);
}
