package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Getter
@Configuration
public class SupabaseStorageConfig {

    // URL base del proyecto Supabase (ej: https://xyzabc.supabase.co)
    @Value("${supabase.url}")
    private String url;

    // Service Role Key de Supabase (con permisos de escritura en Storage)
    @Value("${supabase.key}")
    private String key;

    // Nombre del bucket configurado en Supabase Storage
    @Value("${supabase.bucket:verificacion-docs}")
    private String bucket;
}