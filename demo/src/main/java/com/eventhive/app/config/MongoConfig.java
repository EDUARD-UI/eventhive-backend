package com.eventhive.app.config;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfig {

    /**
     * Limita el pool de conexiones de MongoDB para optimizar drásticamente el consumo
     * de memoria en entornos con recursos limitados (Render / Docker 512MB RAM).
     * Por defecto el driver de MongoDB reserva hasta 100 conexiones con sus respectivos
     * buffers de sockets y monitoreo.
     */
    @Bean
    public MongoClientSettingsBuilderCustomizer mongoConnectionPoolCustomizer() {
        return builder -> builder.applyToConnectionPoolSettings(pool -> pool
                .maxSize(5)
                .minSize(1)
                .maxWaitTime(5, TimeUnit.SECONDS)
                .maxConnectionIdleTime(30, TimeUnit.SECONDS));
    }
}
