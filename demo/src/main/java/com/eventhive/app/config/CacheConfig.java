package com.eventhive.app.config;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_BANNERS = "banners";
    public static final String CACHE_CATEGORIAS = "categorias";
    public static final String CACHE_EVENTOS_PUBLICOS = "eventosPublicos";
    public static final String CACHE_EVENTOS_CARRUSEL = "eventosCarrusel";
    public static final String CACHE_EVENTOS_DESTACADOS = "eventosDestacados";
    public static final String CACHE_PRIMER_DESTACADO = "primerDestacado";
    public static final String CACHE_EVENTOS_MAPA = "eventosMapa";
    public static final String CACHE_RUT_FIRMADO = "rutFirmado";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        List<CaffeineCache> caches = List.of(
                buildCache(CACHE_BANNERS, 24, TimeUnit.HOURS, 50),
                buildCache(CACHE_CATEGORIAS, 24, TimeUnit.HOURS, 100),
                buildCache(CACHE_EVENTOS_PUBLICOS, 30, TimeUnit.MINUTES, 500),
                buildCache(CACHE_EVENTOS_CARRUSEL, 30, TimeUnit.MINUTES, 50),
                buildCache(CACHE_EVENTOS_DESTACADOS, 30, TimeUnit.MINUTES, 100),
                buildCache(CACHE_PRIMER_DESTACADO, 15, TimeUnit.MINUTES, 20),
                buildCache(CACHE_EVENTOS_MAPA, 15, TimeUnit.MINUTES, 200),
                buildCache(CACHE_RUT_FIRMADO, 50, TimeUnit.MINUTES, 200)
        );
        cacheManager.setCaches(caches);
        return cacheManager;
    }

    private CaffeineCache buildCache(String name, long duration, TimeUnit unit, long maximumSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(duration, unit)
                .maximumSize(maximumSize)
                .recordStats()
                .build());
    }
}
