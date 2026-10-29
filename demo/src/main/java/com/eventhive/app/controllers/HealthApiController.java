package com.eventhive.app.controllers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthApiController {

    private static final String STATUS_UP_JSON = "{\"status\":\"UP\"}";

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${RENDER_EXTERNAL_URL:${BACKEND_URL:${SERVER_URL:}}}")
    private String serverExternalUrl;

    // Instancia única y ligera de HttpClient para no generar hilos ni consumir memoria innecesaria
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    @GetMapping(value = {"/api/health", "/health"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> health() {
        return ResponseEntity.ok(STATUS_UP_JSON);
    }

    /**
     * Tarea programada cada 13 minutos (780,000 ms) para mantener activo el servidor
     * en entornos de nube (como Render) y evitar que entre en suspensión.
     */
    @Scheduled(fixedRate = 780000, initialDelay = 60000)
    public void ejecutarKeepAlive() {
        try {
            String targetUrl;
            if (serverExternalUrl != null && !serverExternalUrl.isBlank()) {
                String cleanUrl = serverExternalUrl.trim().replaceAll("/+$", "");
                targetUrl = cleanUrl + "/api/health";
            } else {
                targetUrl = "http://localhost:" + serverPort + "/api/health";
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            // Descarta el cuerpo de la respuesta para consumir cero memoria
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // Manejo silencioso: no satura logs ni bloquea la aplicación
        }
    }
}

