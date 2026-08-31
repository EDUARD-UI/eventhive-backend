package com.eventhive.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
        private String permisos;
    }
}
