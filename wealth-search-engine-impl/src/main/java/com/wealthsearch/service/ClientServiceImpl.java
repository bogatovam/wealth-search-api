package com.wealthsearch.service;

import com.wealthsearch.api.ClientService;
import com.wealthsearch.db.repository.ClientRepository;
import com.wealthsearch.model.Client;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public Client createClient(Client client) {
        Objects.requireNonNull(client, "client must not be null");

        Client normalized = client.toBuilder()
            .id(null)
            .email(client.getEmail() != null ? client.getEmail().toLowerCase() : null)
            .build();

        return clientRepository.save(normalized);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Client> findById(UUID clientId) {
        return clientRepository.findById(clientId);
    }
}
