package com.wealthsearch.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Value
@AllArgsConstructor
@Builder(toBuilder = true)
@Schema(description = "Search result entry containing document match")
public class DocumentSearchHit {

    @Schema(description = "Matched document entity")
    Document document;

    @Schema(description = "Score of the match", example = "0.85")
    double score;
}
