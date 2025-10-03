package com.wealthsearch.client.ollama;

import com.wealthsearch.client.ollama.dto.OllamaEmbedResponse;
import com.wealthsearch.client.ollama.dto.OllamaGenerateResponse;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class OllamaResponseMapper {

    private OllamaResponseMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static OllamaGenerateResponse mapGenerateResponse(OllamaApi.ChatResponse response) {
        if (response == null) {
            return new OllamaGenerateResponse();
        }

        String messageContent = Optional.ofNullable(response.message())
                                        .map(OllamaApi.Message::content)
                                        .orElse(null);

        String createdAt = Optional.ofNullable(response.createdAt())
                                   .map(Instant::toString)
                                   .orElse(null);

        return OllamaGenerateResponse.builder()
                                     .model(response.model())
                                     .createdAt(createdAt)
                                     .response(messageContent)
                                     .done(response.done())
                                     .totalDuration(response.totalDuration())
                                     .loadDuration(response.loadDuration())
                                     .promptEvalCount(response.promptEvalCount())
                                     .promptEvalDuration(response.promptEvalDuration())
                                     .evalCount(response.evalCount())
                                     .evalDuration(response.evalDuration())
                                     .build();
    }

    public static OllamaEmbedResponse mapEmbedResponse(OllamaApi.EmbeddingsResponse response) {
        if (response == null || response.embeddings() == null || response.embeddings()
                                                                         .isEmpty()) {
            return new OllamaEmbedResponse(List.of());
        }

        float[] rawEmbedding = response.embeddings()
                                       .getFirst();
        List<Double> converted = IntStream.range(0, rawEmbedding.length)
                                          .mapToDouble(i -> rawEmbedding[i])
                                          .boxed()
                                          .collect(Collectors.toList());

        return new OllamaEmbedResponse(converted);
    }
}
