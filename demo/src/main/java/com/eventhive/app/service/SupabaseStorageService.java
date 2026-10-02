package com.eventhive.app.service;

import java.io.IOException;
import java.io.InputStream;
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

    private static final long MAX_FILE_SIZE =
            5L * 1024 * 1024;

        private static final int HEADER_BUFFER_SIZE = 12;

    private final SupabaseStorageConfig config;

    private final RestTemplate restTemplate;


    // =========================================================
    // DOCUMENTOS DE VERIFICACIÓN
    // =========================================================

    public String subirDocumentoVerificacion(
            MultipartFile archivo
    ) {

        validarDocumento(archivo);

        return subirArchivo(
                archivo,
                config.getBucketVerificaciones(),
                "verificacion_",
                false
        );
    }


    public String generarUrlFirmada(
            String referencia,
            long expiresInSeconds
    ) {

        if (referencia == null ||
                referencia.isBlank()) {

            throw new BusinessException(
                    "La referencia del archivo es requerida"
            );
        }

        if (expiresInSeconds <= 0) {

            throw new BusinessException(
                    "El tiempo de expiración debe ser mayor que cero"
            );
        }

        String bucket =
                config.getBucketVerificaciones();

        String nombreArchivo =
                referencia;

        String prefijo =
                bucket + "/";

        if (referencia.startsWith(prefijo)) {

            nombreArchivo =
                    referencia.substring(
                            prefijo.length()
                    );

        } else if (
                referencia.contains(
                        "/object/public/"
                                + bucket
                                + "/"
                )
        ) {

            String parte =
                    "/object/public/"
                            + bucket
                            + "/";

            nombreArchivo =
                    referencia.substring(
                            referencia.indexOf(parte)
                                    + parte.length()
                    );
        }

        String url =
                config.getUrl()
                        + "/storage/v1/object/sign/"
                        + bucket
                        + "/"
                        + nombreArchivo;

        HttpHeaders headers =
                construirHeaders(
                        MediaType.APPLICATION_JSON
                );

        HttpEntity<Map<String, Long>> request =
                new HttpEntity<>(
                        Map.of(
                                "expiresIn",
                                expiresInSeconds
                        ),
                        headers
                );

        ResponseEntity<Map> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        request,
                        Map.class
                );

        Object signedUrl =
                response.getBody() == null
                        ? null
                        : response.getBody()
                        .get("signedURL");

        if (!(signedUrl instanceof String)
                || ((String) signedUrl).isBlank()) {

            throw new BusinessException(
                    "Supabase no devolvió una URL firmada válida"
            );
        }

        String value =
                (String) signedUrl;

        return value.startsWith("http")
                ? value
                : config.getUrl() + value;
    }


    public void eliminarDocumentoVerificacion(
            String referencia
    ) {

        if (referencia == null ||
                referencia.isBlank()) {

            return;
        }

        String prefijo =
                config.getBucketVerificaciones()
                        + "/";

        String nombreArchivo =
                referencia.startsWith(prefijo)
                        ? referencia.substring(
                        prefijo.length()
                )
                        : extraerNombreArchivo(
                        referencia
                );

        eliminarArchivo(
                config.getBucketVerificaciones(),
                nombreArchivo
        );
    }


    // =========================================================
    // IMÁGENES
    // =========================================================

    public String subirImagenEvento(
            MultipartFile archivo
    ) {

        validarImagen(archivo);

        return subirArchivo(
                archivo,
                config.getBucketEventos(),
                "evento_",
                true
        );
    }


    public String subirImagenCategoria(
            MultipartFile archivo
    ) {

        validarImagen(archivo);

        return subirArchivo(
                archivo,
                config.getBucketCategorias(),
                "categoria_",
                true
        );
    }

    public String subirImagenBannerHome(
            MultipartFile archivo,
            int posicion
    ) {

        validarImagen(archivo, true);

        return subirArchivo(
                archivo,
                config.getBucketBannersHome(),
                "",
                true,
                "banner-" + posicion
        );
    }

    public String subirImagenPerfilOrganizacion(
            MultipartFile archivo
    ) {

        validarImagen(archivo, false);

        return subirArchivo(
                archivo,
                config.getBucketPerfilOrganizacion(),
                "org_",
                true
        );
    }

    public String subirImagenPerfilUsuario(
            MultipartFile archivo
    ) {

        validarImagen(archivo, false);

        return subirArchivo(
                archivo,
                config.getBucketImagenPerfil(),
                "user_",
                true
        );
    }

    public void eliminarImagenDeBucket(
            String bucket,
            String url
    ) {
        if (url == null || url.isBlank() || bucket == null || bucket.isBlank()) {
            return;
        }
        String nombre = extraerNombreArchivo(url);
        if (nombre != null && !nombre.isBlank()) {
            eliminarArchivo(bucket, nombre);
        }
    }


    // =========================================================
    // ELIMINAR ARCHIVOS
    // =========================================================

    public void eliminarArchivo(
            String bucket,
            String nombreArchivo
    ) {

        if (bucket == null ||
                bucket.isBlank() ||
                nombreArchivo == null ||
                nombreArchivo.isBlank()) {

            return;
        }

        String url =
                config.getUrl()
                        + "/storage/v1/object/"
                        + bucket
                        + "/"
                        + nombreArchivo;

        HttpHeaders headers =
                construirHeaders(
                        MediaType.APPLICATION_JSON
                );

        try {

            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    new HttpEntity<>(headers),
                    Void.class
            );

        } catch (RuntimeException e) {

            /*
             * El fallo al eliminar un archivo viejo
             * no debe tumbar la operación principal.
             */
            log.warn(
                    "No se pudo eliminar archivo en Supabase: {}",
                    e.getMessage()
            );
        }
    }


    public String extraerNombreArchivo(
            String urlPublica
    ) {

        if (urlPublica == null ||
                urlPublica.isBlank()) {

            return null;
        }

        int ultimaBarra =
                urlPublica.lastIndexOf("/");

        if (ultimaBarra < 0 ||
                ultimaBarra == urlPublica.length() - 1) {

            return null;
        }

        return urlPublica.substring(
                ultimaBarra + 1
        );
    }


    // =========================================================
    // SUBIDA
    // =========================================================

    private String subirArchivo(
            MultipartFile archivo,
            String bucket,
            String prefijo,
            boolean publico
    ) {

        return subirArchivo(archivo, bucket, prefijo, publico, null);
    }

    private String subirArchivo(
            MultipartFile archivo,
            String bucket,
            String prefijo,
            boolean publico,
            String nombreFijo
    ) {

        if (archivo == null ||
                archivo.isEmpty()) {

            throw new BusinessException(
                    "El archivo no puede estar vacío"
            );
        }

        try {

            String extension =
                    obtenerExtension(
                            archivo.getOriginalFilename()
                    );

            String nombreArchivo = nombreFijo != null
                    ? nombreFijo
                    : prefijo + UUID.randomUUID() + extension;

            String url =
                    config.getUrl()
                            + "/storage/v1/object/"
                            + bucket
                            + "/"
                            + nombreArchivo;

            MediaType contentType =
                    resolverContentType(
                            archivo.getContentType()
                    );

            HttpHeaders headers =
                    construirHeaders(
                            contentType
                    );
            if (nombreFijo != null) {
                headers.set("x-upsert", "true");
            }

            /*
             * MultipartFile ya está respaldado por el
             * almacenamiento temporal de Spring.
             *
             * Se lee una sola vez para enviarlo a Supabase.
             */
            byte[] contenido =
                    archivo.getBytes();

            HttpEntity<byte[]> request =
                    new HttpEntity<>(
                            contenido,
                            headers
                    );

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.PUT,
                            request,
                            String.class
                    );

            if (!response.getStatusCode()
                    .is2xxSuccessful()) {

                throw new BusinessException(
                        "Supabase respondió con error: "
                                + response.getStatusCode()
                );
            }

            if (publico) {

                return config.getUrl()
                        + "/storage/v1/object/public/"
                        + bucket
                        + "/"
                        + nombreArchivo;
            }

            return bucket
                    + "/"
                    + nombreArchivo;

        } catch (IOException e) {

            throw new BusinessException(
                    "No se pudo leer el archivo: "
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // HEADERS
    // =========================================================

    private HttpHeaders construirHeaders(
            MediaType contentType
    ) {

        HttpHeaders headers =
                new HttpHeaders();

        headers.set(
                "apikey",
                config.getKey()
        );

        headers.set(
                "Authorization",
                "Bearer " + config.getKey()
        );

        headers.setContentType(
                contentType
        );

        return headers;
    }


    // =========================================================
    // CONTENT TYPE
    // =========================================================

    private MediaType resolverContentType(
            String contentType
    ) {

        if (contentType == null ||
                contentType.isBlank()) {

            throw new BusinessException(
                    "El tipo de archivo es requerido"
            );
        }

        try {

            return MediaType.parseMediaType(
                    contentType
            );

        } catch (InvalidMediaTypeException ex) {

            throw new BusinessException(
                    "Tipo de archivo inválido"
            );
        }
    }


    // =========================================================
    // EXTENSIÓN
    // =========================================================

    private String obtenerExtension(
            String nombre
    ) {

        if (nombre == null ||
                nombre.isBlank()) {

            return "";
        }

        int posicion =
                nombre.lastIndexOf(".");

        if (posicion < 0) {

            return "";
        }

        return nombre
                .substring(posicion)
                .toLowerCase();
    }


    // =========================================================
    // VALIDACIÓN DE IMÁGENES
    // =========================================================

    private void validarImagen(
            MultipartFile archivo
    ) {

        validarImagen(archivo, false);
    }

    private void validarImagen(
            MultipartFile archivo,
            boolean permiteWebp
    ) {

        if (archivo == null ||
                archivo.isEmpty()) {

            throw new BusinessException(
                    "El archivo de imagen no puede estar vacío"
            );
        }

        if (archivo.getSize() >
                MAX_FILE_SIZE) {

            throw new BusinessException(
                    "La imagen no puede superar los 5MB"
            );
        }

        String contentType =
                archivo.getContentType();

        boolean tipoValido = "image/png".equals(contentType)
                || "image/jpeg".equals(contentType)
                || (permiteWebp && "image/webp".equals(contentType));
        if (contentType == null ||
                !tipoValido) {

            throw new BusinessException(
                    permiteWebp
                            ? "La imagen debe ser PNG, JPG o WebP"
                            : "La imagen debe ser PNG o JPG"
            );
        }

        validarExtension(
                archivo,
                contentType,
                false,
                permiteWebp
        );

        validarFirmaArchivo(
                archivo,
                contentType
        );
    }


    // =========================================================
    // VALIDACIÓN DE DOCUMENTOS
    // =========================================================

    private void validarDocumento(
            MultipartFile archivo
    ) {

        if (archivo == null ||
                archivo.isEmpty()) {

            throw new BusinessException(
                    "El documento no puede estar vacío"
            );
        }

        if (archivo.getSize() >
                MAX_FILE_SIZE) {

            throw new BusinessException(
                    "El documento no puede superar los 5MB"
            );
        }

        String contentType =
                archivo.getContentType();

        if (contentType == null ||
                (!contentType.equals("application/pdf")
                        && !contentType.equals("image/png")
                        && !contentType.equals("image/jpeg"))) {

            throw new BusinessException(
                    "El documento debe ser PDF, PNG o JPG"
            );
        }

        validarExtension(
                archivo,
                contentType,
                true,
                false
        );

        validarFirmaArchivo(
                archivo,
                contentType
        );
    }


    // =========================================================
    // EXTENSIÓN
    // =========================================================

    private void validarExtension(
            MultipartFile archivo,
            String contentType,
            boolean permitePdf,
            boolean permiteWebp
    ) {

        String extension =
                obtenerExtension(
                        archivo.getOriginalFilename()
                );

        boolean valida;

        if (contentType.equals(
                "application/pdf"
        )) {

            valida =
                    permitePdf
                            && extension.equals(".pdf");

        } else if (contentType.equals(
                "image/png"
        )) {

            valida =
                    extension.equals(".png");

        } else if (contentType.equals(
                "image/jpeg"
        )) {

            valida =
                    extension.equals(".jpg")
                            || extension.equals(".jpeg");

                } else if (contentType.equals("image/webp")) {

                        valida = permiteWebp && extension.equals(".webp");

        } else {

            valida = false;
        }

        if (!valida) {

            throw new BusinessException(
                    "La extensión del archivo "
                            + "no coincide con su tipo permitido"
            );
        }
    }


    // =========================================================
    // FIRMA REAL DEL ARCHIVO
    // =========================================================

    private void validarFirmaArchivo(
            MultipartFile archivo,
            String contentType
    ) {

        try (InputStream inputStream =
                     archivo.getInputStream()) {

            byte[] header =
                    new byte[HEADER_BUFFER_SIZE];

            int bytesLeidos =
                    inputStream.read(header);

            boolean valido = false;

            if (contentType.equals(
                    "application/pdf"
            )) {

                valido =
                        bytesLeidos >= 4
                                && header[0] == '%'
                                && header[1] == 'P'
                                && header[2] == 'D'
                                && header[3] == 'F';

            } else if (contentType.equals(
                    "image/png"
            )) {

                valido =
                        bytesLeidos >= 8
                                && (header[0] & 0xFF) == 0x89
                                && (header[1] & 0xFF) == 0x50
                                && (header[2] & 0xFF) == 0x4E
                                && (header[3] & 0xFF) == 0x47
                                && (header[4] & 0xFF) == 0x0D
                                && (header[5] & 0xFF) == 0x0A
                                && (header[6] & 0xFF) == 0x1A
                                && (header[7] & 0xFF) == 0x0A;

            } else if (contentType.equals(
                    "image/jpeg"
            )) {

                valido =
                        bytesLeidos >= 3
                                && (header[0] & 0xFF) == 0xFF
                                && (header[1] & 0xFF) == 0xD8
                                && (header[2] & 0xFF) == 0xFF;

            } else if (contentType.equals("image/webp")) {

                valido = bytesLeidos >= 12
                        && header[0] == 'R'
                        && header[1] == 'I'
                        && header[2] == 'F'
                        && header[3] == 'F'
                        && header[8] == 'W'
                        && header[9] == 'E'
                        && header[10] == 'B'
                        && header[11] == 'P';
            }

            if (!valido) {

                throw new BusinessException(
                        "El contenido real del archivo "
                                + "no es válido"
                );
            }

        } catch (IOException e) {

            throw new BusinessException(
                    "No se pudo validar el contenido "
                            + "del archivo"
            );
        }
    }
}