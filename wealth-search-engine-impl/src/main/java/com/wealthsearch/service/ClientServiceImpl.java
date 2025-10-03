package com.wealthsearch.service;

import com.wealthsearch.api.ClientService;
import com.wealthsearch.db.repository.ClientRepository;
import com.wealthsearch.model.exception.DuplicateClientEmailException;
import com.wealthsearch.model.entity.Client;
import com.wealthsearch.model.exception.ClientAlreadyExistsException;
import java.util.Locale;
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

        Client normalized = client.toBuilder()
                                  .id(null)
                                  .email(Optional.ofNullable(client.getEmail())
                                                 .map(String::toLowerCase)
                                                 .orElse(null))
                                  .countryOfResidence(Optional.ofNullable(client.getCountryOfResidence())
                                                              .map(code -> code.toUpperCase(Locale.ROOT))
                                                              .orElse(null))
                                  .build();

        try {
            return clientRepository.save(normalized);
        } catch (DuplicateClientEmailException ex) {
            throw new ClientAlreadyExistsException(normalized.getEmail());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Client> findById(UUID clientId) {
        return clientRepository.findById(clientId);
    }
}
