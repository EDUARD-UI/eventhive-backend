package com.eventhive.app.service;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.eventhive.app.config.SupabaseStorageConfig;
import com.eventhive.app.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupabaseStorageService {

    private final SupabaseStorageConfig config;
    private final RestTemplate restTemplate;

    //CARGA DE IMAGENES Y DOCUMENTOS AL STORAGE
    public String subirDocumentoVerificacion(MultipartFile archivo) {
        validarDocumento(archivo);
        return subirArchivo(archivo, config.getBucketVerificaciones(), "verificacion_", false);
    }

    public String generarUrlFirmada(String referencia, long expiresInSeconds) {
        String bucket = config.getBucketVerificaciones();
        String nombreArchivo = referencia;
        String prefijo = bucket + "/";
        if (referencia.startsWith(prefijo)) {
            nombreArchivo = referencia.substring(prefijo.length());
        } else if (referencia.contains("/object/public/" + bucket + "/")) {
            nombreArchivo = referencia.substring(referencia.indexOf("/object/public/" + bucket + "/")
                    + ("/object/public/" + bucket + "/").length());
        }

        String url = config.getUrl() + "/storage/v1/object/sign/" + bucket + "/" + nombreArchivo;
        HttpHeaders headers = construirHeaders(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Long>> request = new HttpEntity<>(Map.of("expiresIn", expiresInSeconds), headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
        Object signedUrl = response.getBody() == null ? null : response.getBody().get("signedURL");
        if (!(signedUrl instanceof String) || ((String) signedUrl).isBlank()) {
            throw new BusinessException("Supabase no devolvió una URL firmada válida");
        }
        String value = (String) signedUrl;
        return value.startsWith("http") ? value : config.getUrl() + value;
    }

    public void eliminarDocumentoVerificacion(String referencia) {
        if (referencia == null || referencia.isBlank()) return;
        String prefijo = config.getBucketVerificaciones() + "/";
        String nombreArchivo = referencia.startsWith(prefijo)
                ? referencia.substring(prefijo.length()) : extraerNombreArchivo(referencia);
        eliminarArchivo(config.getBucketVerificaciones(), nombreArchivo);
    }

    public String subirImagenEvento(MultipartFile archivo) {
        validarImagen(archivo);
        return subirArchivo(archivo, config.getBucketEventos(), "evento_", true);
    }

    public String subirImagenCategoria(MultipartFile archivo) {
        validarImagen(archivo);
        return subirArchivo(archivo, config.getBucketCategorias(), "categoria_", true);
    }

    public void eliminarArchivo(String bucket, String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) return;

        String url = config.getUrl() + "/storage/v1/object/" + bucket + "/" + nombreArchivo;
        HttpHeaders headers = construirHeaders(MediaType.APPLICATION_JSON);

        try {
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (RuntimeException e) {
            log.warn("No se pudo eliminar archivo en Supabase: {}", e.getMessage(), e);
        }
    }

    public String extraerNombreArchivo(String urlPublica) {
        if (urlPublica == null || urlPublica.isBlank()) return null;
        return urlPublica.substring(urlPublica.lastIndexOf("/") + 1);
    }

    //METODOS AUXILIARES Y VALIDACION
    private String subirArchivo(MultipartFile archivo, String bucket, String prefijo, boolean publico) {
        try {
            String extension = obtenerExtension(archivo.getOriginalFilename());
            String nombreArchivo = prefijo + UUID.randomUUID() + extension;
            String url = config.getUrl() + "/storage/v1/object/" + bucket + "/" + nombreArchivo;

            HttpHeaders headers = construirHeaders(resolverContentType(archivo.getContentType()));
            HttpEntity<byte[]> request = new HttpEntity<>(archivo.getBytes(), headers);
            ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);

            if (!res.getStatusCode().is2xxSuccessful())
                throw new BusinessException("Supabase respondió con error: " + res.getStatusCode());

            if (publico) {
                return config.getUrl() + "/storage/v1/object/public/" + bucket + "/" + nombreArchivo;
            }
            return bucket + "/" + nombreArchivo;

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

    // Valida el content-type recibido del MultipartFile antes de dárselo a MediaType.parseMediaType,
    // que lanza InvalidMediaTypeException (no controlada) ante un valor mal formado.
    private MediaType resolverContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new BusinessException("El tipo de archivo es requerido");
        }

        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException ex) {
            throw new BusinessException("Tipo de archivo inválido");
        }
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
        validarExtensionYContenido(archivo, ct, false);
    }

    private void validarDocumento(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty())
            throw new BusinessException("El documento no puede estar vacío");

        if (archivo.getSize() > 5L * 1024 * 1024)
            throw new BusinessException("El documento no puede superar los 5MB");

        String ct = archivo.getContentType();
        if (ct == null || (!ct.equals("application/pdf")
                && !ct.equals("image/png")
                && !ct.equals("image/jpeg")))
            throw new BusinessException("El documento debe ser PDF, PNG o JPG");
        validarExtensionYContenido(archivo, ct, true);
    }

    private void validarExtensionYContenido(MultipartFile archivo, String contentType, boolean permitePdf) {
        String extension = obtenerExtension(archivo.getOriginalFilename());
        boolean extensionValida = (contentType.equals("application/pdf") && permitePdf && extension.equals(".pdf"))
                || (contentType.equals("image/png") && extension.equals(".png"))
                || (contentType.equals("image/jpeg") && (extension.equals(".jpg") || extension.equals(".jpeg")));
        if (!extensionValida) {
            throw new BusinessException("La extensión del archivo no coincide con su tipo permitido");
        }
        try {
            byte[] bytes = archivo.getBytes();
            boolean contenidoValido = (contentType.equals("application/pdf") && bytes.length >= 4
                    && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F')
                    || (contentType.equals("image/png") && bytes.length >= 8
                    && bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                    && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A)
                    || (contentType.equals("image/jpeg") && bytes.length >= 3
                    && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF);
            if (!contenidoValido) throw new BusinessException("El contenido real del archivo no es válido");
        } catch (IOException e) {
            throw new BusinessException("No se pudo validar el contenido del archivo");
        }
    }
}
