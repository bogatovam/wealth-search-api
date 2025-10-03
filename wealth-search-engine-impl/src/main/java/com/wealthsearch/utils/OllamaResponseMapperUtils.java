package com.wealthsearch.utils;

import com.wealthsearch.model.ollama.OllamaEmbedResponse;
import org.springframework.ai.ollama.api.OllamaApi;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class OllamaResponseMapperUtils {

    private OllamaResponseMapperUtils() {
        throw new UnsupportedOperationException("Utility class");
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
