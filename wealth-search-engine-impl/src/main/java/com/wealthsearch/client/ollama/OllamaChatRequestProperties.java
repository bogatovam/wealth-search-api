package com.wealthsearch.client.ollama;

import lombok.Data;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.ai.ollama.chat.request")
public class OllamaChatRequestProperties {

    private boolean stream;

    private String format;

    private OllamaOptions options;

    public OllamaOptions copyOptions() {
        if (options == null) {
            return null;
        }

        return OllamaOptions.builder()
                            .temperature(options.getTemperature())
                            .numPredict(options.getNumPredict())
                            .topK(options.getTopK())
                            .topP(options.getTopP())
                            .repeatPenalty(options.getRepeatPenalty())
                            .seed(options.getSeed())
                            .build();
    }
}
