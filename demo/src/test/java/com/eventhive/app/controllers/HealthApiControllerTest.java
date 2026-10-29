package com.eventhive.app.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class HealthApiControllerTest {

    private HealthApiController controller;

    @BeforeEach
    void setUp() {
        controller = new HealthApiController();
    }

    @Test
    @DisplayName("Debe responder 200 OK con payload estático UP sin consumir memoria")
    void testHealthEndpoint() {
        ResponseEntity<String> response = controller.health();
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\":\"UP\"}", response.getBody());
    }

    @Test
    @DisplayName("La tarea programada debe ejecutarse silenciosamente sin lanzar excepciones")
    void testEjecutarKeepAliveSilencioso() {
        // Ejecuta el keep-alive para verificar que maneja excepciones y no arroja error
        controller.ejecutarKeepAlive();
    }
}

