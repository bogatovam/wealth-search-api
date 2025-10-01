package com.wealthsearch.service;

import com.wealthsearch.api.SearchService;
import com.wealthsearch.db.repository.ClientRepository;
import com.wealthsearch.db.repository.DocumentRepository;
import com.wealthsearch.model.Client;
import com.wealthsearch.model.Document;
import com.wealthsearch.model.SearchHit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ClientRepository clientRepository;
    private final DocumentRepository documentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SearchHit> search(String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }

        List<Client> domainMatches = clientRepository.findByEmailDomainFragment(trimmed);
        Map<UUID, Client> clientsById = new LinkedHashMap<>();
        domainMatches.forEach(client -> clientsById.put(client.getId(), client));

        List<Document> documentMatches = documentRepository.searchByContent(trimmed.toLowerCase());
        documentMatches.stream()
            .map(Document::getClientId)
            .filter(id -> !clientsById.containsKey(id))
            .forEach(id -> clientRepository.findById(id).ifPresent(client -> clientsById.put(id, client)));

        List<SearchHit> hits = new ArrayList<>();
        domainMatches.forEach(client -> hits.add(SearchHit.builder()
            .client(client)
            .document(null)
            .score(1.0)
            .build()));

        documentMatches.forEach(doc -> {
            Client owner = clientsById.get(doc.getClientId());
            hits.add(SearchHit.builder()
                .client(owner)
                .document(doc)
                .score(0.8)
                .build());
        });

        return hits;
    }
}
