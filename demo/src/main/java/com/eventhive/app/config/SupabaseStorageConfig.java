package com.eventhive.app.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;


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

    public String getBucketBannersHome() {
        return properties.getBucket().getBannersHome();
    }

    public String getBucketPerfilOrganizacion() {
        return properties.getBucket().getPerfilOrganizacion();
    }

    public String getBucketImagenPerfil() {
        return properties.getBucket().getImagenPerfil();
    }
}
