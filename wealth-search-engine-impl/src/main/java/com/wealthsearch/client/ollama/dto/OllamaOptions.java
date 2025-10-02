package com.wealthsearch.client.ollama.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OllamaOptions {
    private Integer temperature;

    @JsonProperty("num_predict")
    private Integer numPredict;

    @JsonProperty("top_k")
    private Integer topK;

    @JsonProperty("top_p")
    private Double topP;
}
