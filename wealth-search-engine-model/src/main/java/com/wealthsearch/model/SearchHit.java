package com.wealthsearch.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@Schema(name = "SearchHit", description = "Search result entry containing client and document match")
public class SearchHit {

    @Schema(description = "Matched client entity")
    Client client;

    @Schema(description = "Document where the match was found")
    Document document;

    @Schema(description = "Score of the match", example = "0.85")
    double score;
}
