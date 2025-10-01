package com.wealthsearch.api;

import com.wealthsearch.model.Client;
import java.util.Optional;
import java.util.UUID;

public interface ClientService {
    Client createClient(CreateClientCommand command);

    Optional<Client> findById(UUID clientId);
}
