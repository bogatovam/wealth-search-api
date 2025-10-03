package com.wealthsearch.model.entity.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Schema(description = "Search results with pagination metadata")
public class SearchResult<T> {

    @Schema(description = "List of search results")
    List<T> results = new ArrayList<>();

    @Schema(description = "Total number of matching results", example = "42")
    long totalCount = 0;
}
