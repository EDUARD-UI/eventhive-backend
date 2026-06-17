package com.example.demo.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.config.SupabaseStorageConfig;
import com.example.demo.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupabaseStorageService {

    private final SupabaseStorageConfig config;
    private final RestTemplate restTemplate;

    public String subirDocumentoVerificacion(MultipartFile archivo) {
        validarDocumento(archivo);

        return subirArchivo(
                archivo,
                config.getBucketVerificaciones(),
                "verificacion_"
        );
    }

    public String subirImagenEvento(MultipartFile archivo) {
        validarImagen(archivo, 5);

        return subirArchivo(
                archivo,
                config.getBucketEventos(),
                "evento_"
        );
    }

    public String subirImagenCategoria(MultipartFile archivo) {
        validarImagen(archivo, 5);

        return subirArchivo(
                archivo,
                config.getBucketCategorias(),
                "categoria_"
        );
    }

    private String subirArchivo(
            MultipartFile archivo,
            String bucket,
            String prefijo) {

        // lógica común de subida
    }
}