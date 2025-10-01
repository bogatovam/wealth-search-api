package com.wealthsearch.service;

import com.wealthsearch.api.ClientService;
import com.wealthsearch.api.CreateClientCommand;
import com.wealthsearch.db.repository.ClientRepository;
import com.wealthsearch.model.Client;
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
    public Client createClient(CreateClientCommand command) {
        Client client = Client.builder()
            .firstName(command.firstName())
            .lastName(command.lastName())
            .email(command.email().toLowerCase())
            .countryOfResidence(command.countryOfResidence())
            .build();
        return clientRepository.save(client);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Client> findById(UUID clientId) {
        return clientRepository.findById(clientId);
    }
}
