package com.wealthsearch.service;

import com.wealthsearch.api.SearchService;
import com.wealthsearch.db.repository.ClientRepository;
import com.wealthsearch.db.repository.DocumentRepository;
import com.wealthsearch.model.ClientSearchHit;

import java.util.ArrayList;
import java.util.List;

import com.wealthsearch.model.error.ErrorEntry;
import com.wealthsearch.model.exception.BadRequestException;
import com.wealthsearch.service.util.SearchQueryUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ClientRepository clientRepository;

    private final DocumentRepository documentRepository;

    @Value("${search.clients-search.max-query-length:128}")
    private Long maxQueryLength;

    @Override
    @Transactional(readOnly = true)
    public List<ClientSearchHit> searchClientsPerCompanyName(String query) {
        this.validateQuery(query);
        String normalizedQuery = SearchQueryUtils.normalize(query);

        if (normalizedQuery.isEmpty()) {
            throw new BadRequestException("Query does not contain searchable characters");
        }

        return clientRepository.findByEmailDomainFragment(normalizedQuery);
    }

    private void validateQuery(String query) {
        List<ErrorEntry> errors = new ArrayList<>();

        if (StringUtils.isBlank(query)) {
            errors.add(new ErrorEntry("Query should not be empty"));
        } else {
            if (StringUtils.length(query) > maxQueryLength) {
                errors.add(new ErrorEntry("Query is too long. Max allowed length: '%s'".formatted(maxQueryLength)));
            }
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException(errors);
        }
    }
}
