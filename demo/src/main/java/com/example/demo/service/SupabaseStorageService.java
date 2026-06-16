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

    // Sube un archivo al bucket de Supabase y retorna la URL pública
    public String subirArchivo(MultipartFile archivo) {
        validarArchivo(archivo);

        String extension = obtenerExtension(archivo.getOriginalFilename());
        String nombreUnico = "rut_" + UUID.randomUUID() + extension;

        String uploadUrl = config.getUrl()
                + "/storage/v1/object/"
                + config.getBucket() + "/"
                + nombreUnico;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + config.getKey());
        headers.set("apikey", config.getKey());
        headers.setContentType(MediaType.parseMediaType(
                archivo.getContentType() != null ? archivo.getContentType() : "application/octet-stream"
        ));

        byte[] bytes;
        try {
            bytes = archivo.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Error al leer el archivo: " + e.getMessage());
        }

        HttpEntity<byte[]> entity = new HttpEntity<>(bytes, headers);
        ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new BusinessException("Error al subir archivo a Supabase Storage");
        }

        // Retorna la URL pública del archivo subido
        return config.getUrl()
                + "/storage/v1/object/public/"
                + config.getBucket() + "/"
                + nombreUnico;
    }

    // Elimina un archivo del bucket dado su URL pública
    public void eliminarArchivo(String urlPublica) {
        if (urlPublica == null || urlPublica.isBlank()) return;

        String nombreArchivo = urlPublica.substring(urlPublica.lastIndexOf("/") + 1);
        String deleteUrl = config.getUrl()
                + "/storage/v1/object/"
                + config.getBucket() + "/"
                + nombreArchivo;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + config.getKey());
        headers.set("apikey", config.getKey());

        restTemplate.exchange(deleteUrl, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
    }

    // Valida tipo y tamaño del archivo (PDF o imagen, máx 10MB)
    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty())
            throw new BusinessException("El archivo no puede estar vacío");

        if (archivo.getSize() > 10 * 1024 * 1024)
            throw new BusinessException("El archivo no puede superar los 10MB");

        String contentType = archivo.getContentType();
        if (contentType == null ||
                (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
            throw new BusinessException("Solo se permiten imágenes o archivos PDF");
        }
    }

    private String obtenerExtension(String nombreOriginal) {
        if (nombreOriginal != null && nombreOriginal.contains("."))
            return nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
        return "";
    }
}