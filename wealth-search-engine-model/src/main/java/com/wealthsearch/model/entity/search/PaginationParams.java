package com.wealthsearch.model.entity.search;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder(toBuilder = true)
@Schema(description = "Pagination parameters")
public class PaginationParams {

    @Schema(description = "Maximum number of results to return", example = "20")
    @Min(1)
    @Max(100)
    int limit;

    @Schema(description = "Number of results to skip", example = "0")
    @Min(0)
    int offset;

    public static PaginationParams of(int limit, int offset) {
        return new PaginationParams(limit, offset);
    }

    public static PaginationParams defaultPage() {
        return new PaginationParams(20, 0);
    }
}
