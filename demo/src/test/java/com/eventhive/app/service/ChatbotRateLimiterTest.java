package com.eventhive.app.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChatbotRateLimiterTest {

    @Test
    void limitaCincoSolicitudesPorMinutoDeCadaIpDeFormaIndependiente() {
        ChatbotRateLimiter rateLimiter = new ChatbotRateLimiter();

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.permitir("203.0.113.1"));
        }
        assertFalse(rateLimiter.permitir("203.0.113.1"));

        assertTrue(rateLimiter.permitir("203.0.113.2"));
    }
}
