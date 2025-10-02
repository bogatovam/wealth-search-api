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

        return null;
    }
}
