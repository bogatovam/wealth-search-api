package com.wealthsearch.model.entity.search;

import com.wealthsearch.model.entity.Client;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder(toBuilder = true)
@Schema(description = "Search result entry containing client and document match")
public class ClientSearchHit {

    @Schema(description = "Matched client entity")
    Client client;

    @Schema(description = "Score of the match", example = "0.85")
    double score;
}
