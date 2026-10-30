package com.eventhive.app.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Component
public class ChatbotRateLimiter {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_NANOS = TimeUnit.MINUTES.toNanos(1);

    private final Cache<String, RequestWindow> requestWindows = Caffeine.newBuilder()
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public boolean permitir(String ip) {
        if (ip == null || ip.isBlank()) {
            throw new IllegalArgumentException("La dirección IP de la solicitud es obligatoria");
        }

        RequestWindow window = requestWindows.get(ip, ignored -> new RequestWindow());
        long now = System.nanoTime();
        synchronized (window) {
            while (!window.requests.isEmpty()
                    && now - window.requests.peekFirst() >= WINDOW_NANOS) {
                window.requests.removeFirst();
            }

            if (window.requests.size() >= MAX_REQUESTS) {
                return false;
            }

            window.requests.addLast(now);
            return true;
        }
    }

    private static final class RequestWindow {
        private final Deque<Long> requests = new ArrayDeque<>();
    }
}
