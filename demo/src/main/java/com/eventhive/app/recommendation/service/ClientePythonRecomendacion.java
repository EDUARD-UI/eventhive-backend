package com.eventhive.app.recommendation.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.eventhive.app.recommendation.dto.EvaluacionRecomendacionRequest;
import com.eventhive.app.recommendation.dto.EvaluacionRecomendacionResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ClientePythonRecomendacion {

    private final RestClient restClient;

    public ClientePythonRecomendacion(
            RestClient.Builder builder,
            @Value("${recommendation.python.base-url:http://localhost:8000}") String baseUrl,
            @Value("${recommendation.python.service-token:}") String serviceToken
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        RestClient.Builder clientBuilder = builder
            .baseUrl(baseUrl != null ? baseUrl.replaceAll("/+$", "") : "http://localhost:8000")
            .requestFactory(requestFactory);

        if (serviceToken != null && !serviceToken.isBlank()) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken.trim());
        }

        this.restClient = clientBuilder.build();
    }

    public EvaluacionRecomendacionResponse evaluar(EvaluacionRecomendacionRequest request) {
        try {
            return restClient.post()
                .uri("/api/v1/recommendations/evaluate")
                .body(request)
                .retrieve()
                .body(EvaluacionRecomendacionResponse.class);
        } catch (Exception ex) {
            log.warn("No se pudo comunicar con el microservicio Python de recomendaciones: {}", ex.getMessage());
            return null;
        }
    }
}

