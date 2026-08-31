package com.eventhive.app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(SupabaseStorageProperties.class)
public class SupabaseStorageConfig {

    private final SupabaseStorageProperties properties;

    public String getUrl() {
        return properties.getUrl();
    }

    public String getKey() {
        return properties.getKey();
    }

    public String getBucketVerificaciones() {
        return properties.getBucket().getVerificaciones();
    }

    public String getBucketEventos() {
        return properties.getBucket().getEventos();
    }

    public String getBucketCategorias() {
        return properties.getBucket().getCategorias();
    }

    public String getBucketPermisos() {
        return properties.getBucket().getPermisos();
    }
}
