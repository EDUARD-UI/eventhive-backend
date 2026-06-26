package com.eventhive.app.service;

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

import com.eventhive.app.config.SupabaseStorageConfig;
import com.eventhive.app.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupabaseStorageService {

    private final SupabaseStorageConfig config;
    private final RestTemplate restTemplate;

    public String subirDocumentoVerificacion(MultipartFile archivo) {
        validarDocumento(archivo);
        return subirArchivo(archivo, config.getBucketVerificaciones(), "verificacion_");
    }

    public String subirImagenEvento(MultipartFile archivo) {
        validarImagen(archivo);
        return subirArchivo(archivo, config.getBucketEventos(), "evento_");
    }

    public String subirImagenCategoria(MultipartFile archivo) {
        validarImagen(archivo);
        return subirArchivo(archivo, config.getBucketCategorias(), "categoria_");
    }

    public void eliminarArchivo(String bucket, String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) return;

        String url = config.getUrl() + "/storage/v1/object/" + bucket + "/" + nombreArchivo;
        HttpHeaders headers = construirHeaders(MediaType.APPLICATION_JSON);

        try {
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (Exception e) {
            System.err.println("Advertencia: no se pudo eliminar archivo en Supabase: " + e.getMessage());
        }
    }

    public String extraerNombreArchivo(String urlPublica) {
        if (urlPublica == null || urlPublica.isBlank()) return null;
        return urlPublica.substring(urlPublica.lastIndexOf("/") + 1);
    }

    private String subirArchivo(MultipartFile archivo, String bucket, String prefijo) {
        try {
            String extension     = obtenerExtension(archivo.getOriginalFilename());
            String nombreArchivo = prefijo + UUID.randomUUID() + extension;
            String url           = config.getUrl() + "/storage/v1/object/" + bucket + "/" + nombreArchivo;

            HttpHeaders headers        = construirHeaders(MediaType.parseMediaType(archivo.getContentType()));
            HttpEntity<byte[]> request = new HttpEntity<>(archivo.getBytes(), headers);
            ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);

            if (!res.getStatusCode().is2xxSuccessful())
                throw new BusinessException("Supabase respondió con error: " + res.getStatusCode());

            return config.getUrl() + "/storage/v1/object/public/" + bucket + "/" + nombreArchivo;

        } catch (IOException e) {
            throw new BusinessException("No se pudo leer el archivo: " + e.getMessage());
        }
    }

    private HttpHeaders construirHeaders(MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", config.getKey());
        headers.set("Authorization", "Bearer " + config.getKey());
        headers.setContentType(contentType);
        return headers;
    }

    private String obtenerExtension(String nombre) {
        if (nombre != null && nombre.contains("."))
            return nombre.substring(nombre.lastIndexOf(".")).toLowerCase();
        return "";
    }

    private void validarImagen(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty())
            throw new BusinessException("El archivo de imagen no puede estar vacío");

        if (archivo.getSize() > 5L * 1024 * 1024)
            throw new BusinessException("La imagen no puede superar los 5MB");

        String ct = archivo.getContentType();
        if (ct == null || (!ct.equals("image/png") && !ct.equals("image/jpeg")))
            throw new BusinessException("La imagen debe ser PNG o JPG");
    }

    private void validarDocumento(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty())
            throw new BusinessException("El documento no puede estar vacío");

        if (archivo.getSize() > 10L * 1024 * 1024)
            throw new BusinessException("El documento no puede superar los 10MB");

        String ct = archivo.getContentType();
        if (ct == null || (!ct.equals("application/pdf")
                        && !ct.equals("image/png")
                        && !ct.equals("image/jpeg")))
            throw new BusinessException("El documento debe ser PDF, PNG o JPG");
    }
}