package com.eventhive.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Getter
@Configuration
public class SupabaseStorageConfig {

    //url del storage
    @Value("${supabase.url}")
    private String url;

    //clave del storage
    @Value("${supabase.key}")
    private String key;

    //nombre de los buckets
    @Value("${supabase.bucket.verificaciones}")
    private String bucketVerificaciones;

    @Value("${supabase.bucket.eventos}")
    private String bucketEventos;

    @Value("${supabase.bucket.categorias}")
    private String bucketCategorias;
}
