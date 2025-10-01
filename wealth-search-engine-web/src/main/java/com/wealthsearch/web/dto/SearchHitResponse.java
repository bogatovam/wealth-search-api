package com.wealthsearch.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchHitResponse(
    @JsonProperty("client") com.wealthsearch.web.openapi.model.Client client,
    @JsonProperty("document") com.wealthsearch.web.openapi.model.Document document,
    @JsonProperty("score") double score
) {
}
