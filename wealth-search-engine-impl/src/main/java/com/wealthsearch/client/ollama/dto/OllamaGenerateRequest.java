package com.wealthsearch.client.ollama.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OllamaGenerateRequest {
    private String model;
    private String prompt;
    private Boolean stream;
    private String system;
    private List<Integer> context;
    private OllamaOptions options;
}
