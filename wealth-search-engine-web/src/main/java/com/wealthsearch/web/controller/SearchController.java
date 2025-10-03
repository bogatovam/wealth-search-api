package com.wealthsearch.web.controller;

import com.wealthsearch.api.SearchService;
import com.wealthsearch.model.entity.search.ClientSearchHit;
import com.wealthsearch.model.entity.search.DocumentSearchHit;
import com.wealthsearch.model.entity.search.PaginationParams;
import com.wealthsearch.model.entity.search.SearchResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@Validated
@Tag(name = "Search", description = "Client search operations")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/clients")
    @Operation(summary = "Search clients by company name",
            description = "Performs fuzzy search for clients based on company domain extracted from email",
            responses = {
                @ApiResponse(responseCode = "200",
                        description = "Search results with total count in X-Total-Count header",
                        headers = @Header(name = "X-Total-Count", description = "Total number of matching clients",
                                schema = @Schema(type = "integer")))
            })
    public ResponseEntity<List<ClientSearchHit>> search(
            @Parameter(description = "Search query", required = true,
                    example = "neviswealth") @RequestParam("q") String query,
            @Parameter(description = "Maximum number of results to return",
                    example = "20") @RequestParam(value = "limit", defaultValue = "20") @Min(1) @Max(100) int limit,
            @Parameter(description = "Number of results to skip", example = "0") @RequestParam(value = "offset",
                    defaultValue = "0") @Min(0) int offset) {

        PaginationParams paginationParams = PaginationParams.of(limit, offset);
        SearchResult<ClientSearchHit> searchResult = searchService.searchClientsPerCompanyName(query, paginationParams);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(searchResult.getTotalCount()));

        return ResponseEntity.ok()
                             .headers(headers)
                             .body(searchResult.getResults());
    }

    @GetMapping("/documents")
    public ResponseEntity<List<DocumentSearchHit>> searchDocuments(
            @Parameter(description = "Search query", required = true,
                    example = "neviswealth") @RequestParam("q") String query,
            @Parameter(description = "Maximum number of results to return",
                    example = "20") @RequestParam(value = "limit", defaultValue = "20") @Min(1) @Max(100) int limit,
            @Parameter(description = "Number of results to skip", example = "0") @RequestParam(value = "offset",
                    defaultValue = "0") @Min(0) int offset) {

        PaginationParams paginationParams = PaginationParams.of(limit, offset);

        SearchResult<DocumentSearchHit> searchResult =
                searchService.searchDocumentsBySimilarTerms(query, paginationParams);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(searchResult.getTotalCount()));

        return ResponseEntity.ok()
                             .headers(headers)
                             .body(searchResult.getResults());
    }
}
