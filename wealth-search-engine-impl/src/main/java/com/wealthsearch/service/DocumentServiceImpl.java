package com.wealthsearch.service;

import com.wealthsearch.api.CreateDocumentCommand;
import com.wealthsearch.api.DocumentService;
import com.wealthsearch.db.repository.ClientRepository;
import com.wealthsearch.db.repository.DocumentRepository;
import com.wealthsearch.model.Document;
import java.util.List;
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
    public Document createDocument(UUID clientId, CreateDocumentCommand command) {
        clientRepository.findById(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        Document document = Document.builder()
            .clientId(clientId)
            .title(command.title())
            .content(command.content())
            .build();
        return documentRepository.save(document);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findByClientId(UUID clientId) {
        return documentRepository.findByClientId(clientId);
    }
}
