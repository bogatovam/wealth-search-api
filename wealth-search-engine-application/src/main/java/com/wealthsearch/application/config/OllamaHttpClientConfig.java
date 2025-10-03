package com.wealthsearch.application.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class OllamaHttpClientConfig {

    @Bean
    WebClientCustomizer ollamaWebClientTimeoutCustomizer(
            @Value("${ollama.client.connect-timeout:PT5S}") Duration connectTimeout,
            @Value("${ollama.client.response-timeout:PT15S}") Duration responseTimeout,
            @Value("${ollama.client.write-timeout:PT10S}") Duration writeTimeout) {
        return builder -> {
            long connectTimeoutMillis = toPositiveMillis(connectTimeout, 5000);
            long responseTimeoutMillis = toPositiveMillis(responseTimeout, 15000);
            long writeTimeoutMillis = toPositiveMillis(writeTimeout, 10000);

            HttpClient httpClient = HttpClient.create()
                                              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeoutMillis)
                                              .responseTimeout(Duration.ofMillis(responseTimeoutMillis))
                                              .doOnConnected(connection -> {
                                                  connection.addHandlerLast(new ReadTimeoutHandler(
                                                          responseTimeoutMillis, TimeUnit.MILLISECONDS));
                                                  connection.addHandlerLast(new WriteTimeoutHandler(writeTimeoutMillis,
                                                          TimeUnit.MILLISECONDS));
                                              });

            builder.clientConnector(new ReactorClientHttpConnector(httpClient));
        };
    }

    @Bean
    RestClientCustomizer ollamaRestClientTimeoutCustomizer(
            @Value("${ollama.client.connect-timeout:PT5S}") Duration connectTimeout,
            @Value("${ollama.client.read-timeout:PT15S}") Duration readTimeout) {
        return restClientBuilder -> {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout((int) toPositiveMillis(connectTimeout, 5000));
            requestFactory.setReadTimeout((int) toPositiveMillis(readTimeout, 15000));
            restClientBuilder.requestFactory(requestFactory);
        };
    }

    private long toPositiveMillis(Duration duration, long defaultMillis) {
        long millis = duration != null ? duration.toMillis() : defaultMillis;
        if (millis <= 0) {
            millis = defaultMillis;
        }
        return Math.min(Integer.MAX_VALUE, millis);
    }
}
