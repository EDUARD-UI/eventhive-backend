package com.eventhive.app.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.eventhive.app.exception.CorreoException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.model.Message;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CorreoSender {

    private final GmailClientProvider gmailClientProvider;

    @Value("${gmail.sender:${GMAIL_SENDER:${eventhive.mail.remitente:eventhiveContacto@gmail.com}}}")
    private String remitente;

    @Value("${eventhive.mail.nombre-remitente:${MAIL_FROM_NAME:EventHive}}")
    private String nombreRemitente;

    @Value("${gmail.max-reintentos:${eventhive.mail.max-reintentos:2}}")
    private int maxReintentos;

    @jakarta.annotation.PostConstruct
    public void validarConfiguracionAlArrancar() {
        if (!gmailClientProvider.estaConfigurado()) {
            log.warn("ADVERTENCIA GMAIL API: Credenciales no configuradas completamente (GMAIL_CLIENT_ID, GMAIL_CLIENT_SECRET o GMAIL_REFRESH_TOKEN vacíos). Visita /oauth2/authorize/google para vincular la cuenta remitente '{}'.",
                    sanitizarLog(remitente));
        } else {
            log.info("Configuración de Gmail API lista: remitente='{}', nombre='{}' (autenticación OAuth 2.0 activa)",
                    sanitizarLog(remitente), sanitizarLog(nombreRemitente));
        }
    }

    // Síncrono: propaga CorreoException si la Gmail API rechaza el envío tras los reintentos.
    public void enviar(String destino, String asunto, String html) {
        enviar(destino, asunto, html, Map.of());
    }

    public void enviar(String destino, String asunto, String html, Map<String, byte[]> imagenesEnLinea) {
        validarParametros(destino, asunto, html);

        if (imagenesEnLinea == null || imagenesEnLinea.entrySet().stream()
                .anyMatch(entry -> !StringUtils.hasText(entry.getKey()) || entry.getValue() == null
                        || entry.getValue().length == 0)) {
            throw new CorreoException("Las imágenes en línea del correo deben tener identificador y contenido");
        }

        if (!gmailClientProvider.estaConfigurado()) {
            log.error("Fallo de envío Gmail API: GMAIL_CLIENT_ID, GMAIL_CLIENT_SECRET o GMAIL_REFRESH_TOKEN no configurados.");
            throw new CorreoException("El servicio de correo no está disponible: faltan credenciales OAuth 2.0 de Gmail (GMAIL_CLIENT_ID / GMAIL_CLIENT_SECRET / GMAIL_REFRESH_TOKEN). Visita /oauth2/authorize/google para vincular la cuenta.");
        }

        int intentos = Math.max(1, maxReintentos);
        Exception ultimoError = null;

        for (int intento = 1; intento <= intentos; intento++) {
            try {
                // 1. Construir mensaje MIME con soporte HTML y UTF-8
                Session session = Session.getInstance(new Properties());
                MimeMessage mimeMessage = new MimeMessage(session);
                MimeMessageHelper helper = new MimeMessageHelper(
                        mimeMessage, !imagenesEnLinea.isEmpty(), StandardCharsets.UTF_8.name());
                helper.setFrom(remitente, nombreRemitente);
                helper.setTo(destino.trim());
                helper.setSubject(asunto.trim());
                helper.setText(html, true);
                for (Map.Entry<String, byte[]> imagen : imagenesEnLinea.entrySet()) {
                    helper.addInline(imagen.getKey(), new ByteArrayResource(imagen.getValue()), "image/png");
                }

                // 2. Serializar a bytes y codificar a Base64 URL-safe (sin padding) requerido por Gmail API
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                mimeMessage.writeTo(buffer);
                String rawBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray());

                // 3. Crear mensaje de Gmail API
                Message gmailMessage = new Message();
                gmailMessage.setRaw(rawBase64Url);

                // 4. Enviar mediante users.messages.send para el usuario autenticado "me"
                Message respuesta = gmailClientProvider.obtenerCliente()
                        .users()
                        .messages()
                        .send("me", gmailMessage)
                        .execute();

                if (respuesta == null || !StringUtils.hasText(respuesta.getId())) {
                    throw new CorreoException("Gmail API no retornó ID de mensaje en la respuesta");
                }

                log.info("Correo '{}' enviado exitosamente vía Gmail API a {} (id: {})",
                        sanitizarLog(asunto), sanitizarLog(destino), sanitizarLog(respuesta.getId()));
                return;

            } catch (Exception e) {
                ultimoError = e;

                if (e instanceof GoogleJsonResponseException gjre) {
                    int statusCode = gjre.getStatusCode();
                    String errorMsg = gjre.getDetails() != null ? gjre.getDetails().getMessage() : gjre.getMessage();
                    if (statusCode == 401) {
                        log.error("Error de autenticación Gmail API (401): Tokens inválidos o revocados. Se requiere reautorizar en /oauth2/authorize/google");
                    } else if (statusCode == 403) {
                        log.error("Error de permisos o cuota en Gmail API (403): {}", sanitizarLog(errorMsg));
                    } else {
                        log.error("Error devuelto por Gmail API (HTTP {}): {}", statusCode, sanitizarLog(errorMsg));
                    }
                } else {
                    GmailClientProvider.OAuthDiagnostico diag = GmailClientProvider.analizarErrorOAuth(e);
                    if (!"DESCONOCIDO".equals(diag.codigoOAuth()) || diag.httpStatus() > 0) {
                        log.error("Error de credenciales/OAuth al enviar correo vía Gmail API [HTTP {} - {}]: {}. Acción: {}",
                                diag.httpStatus(), diag.codigoOAuth(), diag.descripcion(), diag.recomendacion());
                    } else {
                        log.error("Fallo en intento {}/{} al enviar correo '{}' a {}. Causa: {}",
                                intento, intentos, sanitizarLog(asunto), sanitizarLog(destino), sanitizarLog(e.getMessage()), e);
                    }
                }

                if (intento < intentos) {
                    try {
                        Thread.sleep(500L * intento);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        String detalleError;
        if (ultimoError != null) {
            GmailClientProvider.OAuthDiagnostico diag = GmailClientProvider.analizarErrorOAuth(ultimoError);
            if (!"DESCONOCIDO".equals(diag.codigoOAuth()) || diag.httpStatus() > 0) {
                detalleError = "Fallo de autorización OAuth con Google [HTTP " + diag.httpStatus() + " " + diag.codigoOAuth() + "]: "
                        + diag.descripcion() + " -> " + diag.recomendacion();
            } else {
                detalleError = ultimoError.getMessage();
            }
        } else {
            detalleError = "Fallo de conexión o autorización con Gmail API";
        }
        throw new CorreoException("No se pudo enviar el correo a " + destino.trim() + " vía Gmail API: " + detalleError, ultimoError);
    }

    // Asíncrono: para confirmaciones y notificaciones que no deben romper la transacción de negocio.
    @Async("emailExecutor")
    public void enviarAsync(String destino, String asunto, String html) {
        enviarAsync(destino, asunto, html, Map.of());
    }

    @Async("emailExecutor")
    public void enviarAsync(String destino, String asunto, String html, Map<String, byte[]> imagenesEnLinea) {
        try {
            enviar(destino, asunto, html, imagenesEnLinea);
        } catch (CorreoException e) {
            log.error("Error en envío asíncrono '{}' a {}: {}", sanitizarLog(asunto), sanitizarLog(destino), sanitizarLog(e.getMessage()));
        }
    }

    private void validarParametros(String destino, String asunto, String html) {
        if (!StringUtils.hasText(destino) || !destino.contains("@")) {
            throw new CorreoException("El destinatario de correo es inválido o está vacío");
        }
        if (!StringUtils.hasText(asunto)) {
            throw new CorreoException("El asunto del correo no puede estar vacío");
        }
        if (!StringUtils.hasText(html)) {
            throw new CorreoException("El contenido del correo no puede estar vacío");
        }
    }

    private String sanitizarLog(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[\r\n]", "_");
    }
}