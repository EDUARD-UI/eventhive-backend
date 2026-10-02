package com.eventhive.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "supabase")
public class SupabaseStorageProperties {

    private String url;
    private String key;
    private Bucket bucket = new Bucket();

    @Getter
    @Setter
    public static class Bucket {
        private String verificaciones;
        private String eventos;
        private String categorias;
        private String bannersHome = "banners-home";
        private String perfilOrganizacion = "perfilOrganizacion";
        private String imagenPerfil = "imagenPerfil";
    }
}
