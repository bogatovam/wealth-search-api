package com.wealthsearch.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record CreateDocumentRequest(
    @JsonProperty("title") @NotBlank String title,
    @JsonProperty("content") @NotBlank String content
) {
}
