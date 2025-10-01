package com.wealthsearch.web.controller;

import com.wealthsearch.api.SearchService;
import com.wealthsearch.model.SearchHit;
import com.wealthsearch.web.dto.SearchHitResponse;
import com.wealthsearch.web.mapper.ClientMapper;
import com.wealthsearch.web.mapper.DocumentMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@Validated
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public List<SearchHitResponse> search(@RequestParam("q") String query) {
        return searchService.search(query).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    private SearchHitResponse toResponse(SearchHit hit) {
        return new SearchHitResponse(
            ClientMapper.toApi(hit.getClient()),
            DocumentMapper.toApi(hit.getDocument()),
            hit.getScore()
        );
    }
}
