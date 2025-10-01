package com.wealthsearch.service;

import com.wealthsearch.api.DocumentService;
import com.wealthsearch.db.repository.ClientRepository;
import com.wealthsearch.db.repository.DocumentRepository;
import com.wealthsearch.model.Document;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public Document createDocument(Document document) {
        Objects.requireNonNull(document, "document must not be null");

        UUID clientId = document.getClientId();
        if (clientId == null) {
            throw new IllegalArgumentException("Client id must be provided");
        }

        clientRepository.findById(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        Document toPersist = document.toBuilder()
            .id(null)
            .clientId(clientId)
            .build();

        return documentRepository.save(toPersist);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findByClientId(UUID clientId) {
        return documentRepository.findByClientId(clientId);
    }
}
