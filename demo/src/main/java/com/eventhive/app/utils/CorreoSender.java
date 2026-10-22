package com.eventhive.app.utils;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.eventhive.app.exception.CorreoException;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CorreoSender {

    private final JavaMailSender mailSender;

    @Value("${eventhive.mail.remitente:${MAIL_FROM:notificaciones@eventhive.live}}")
    private String remitente;

    @Value("${eventhive.mail.nombre-remitente:${MAIL_FROM_NAME:EventHive}}")
    private String nombreRemitente;

    @Value("${eventhive.mail.max-reintentos:2}")
    private int maxReintentos;

    @Value("${spring.mail.host:smtp-relay.brevo.com}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @jakarta.annotation.PostConstruct
    public void validarConfiguracionAlArrancar() {
        if (!StringUtils.hasText(mailUsername) || !StringUtils.hasText(mailPassword)) {
            log.warn("ADVERTENCIA SMTP: Credenciales no configuradas (MAIL_USERNAME / MAIL_PASSWORD vacías). Los envíos reales de correo fallarán hasta que se configuren en las variables de entorno de Render.");
        } else {
            log.info("Configuración SMTP de Brevo lista: host='{}', puerto={}, remitente='{}' (autenticación configurada)",
                    sanitizarLog(mailHost), mailPort, sanitizarLog(remitente));
        }
    }

    // Síncrono: propaga CorreoException si el servidor SMTP rechaza el correo tras los reintentos.
    public void enviar(String destino, String asunto, String html) {
        validarParametros(destino, asunto, html);

        if (!StringUtils.hasText(mailUsername) || !StringUtils.hasText(mailPassword)) {
            log.error("Fallo de envío SMTP: Variables de entorno MAIL_USERNAME o MAIL_PASSWORD no configuradas en Render.");
            throw new CorreoException("El servicio de correo no está disponible: faltan credenciales SMTP (MAIL_USERNAME / MAIL_PASSWORD)");
        }

        if (remitente != null && (remitente.endsWith(".local") || remitente.endsWith("@localhost"))) {
            log.error("ATENCIÓN: Se intentó enviar correo con remitente ficticio/local '{}'. Brevo rechazará este envío en producción.", remitente);
        }

        int intentos = Math.max(1, maxReintentos);
        Exception ultimoError = null;

        for (int intento = 1; intento <= intentos; intento++) {
            try {
                MimeMessage mensaje = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, StandardCharsets.UTF_8.name());
                helper.setFrom(remitente, nombreRemitente);
                helper.setTo(destino.trim());
                helper.setSubject(asunto.trim());
                helper.setText(html, true);

                mailSender.send(mensaje);
                log.info("Correo '{}' aceptado por servidor SMTP para {}", sanitizarLog(asunto), sanitizarLog(destino));
                return;
            } catch (Exception e) {
                ultimoError = e;
                log.error("Fallo en intento {}/{} al enviar correo '{}' a {}. Causa: {}",
                        intento, intentos, sanitizarLog(asunto), sanitizarLog(destino), sanitizarLog(e.getMessage()), e);

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
        String detalleError = ultimoError != null ? ultimoError.getMessage() : "Fallo de conexión o autenticación SMTP";
        throw new CorreoException("No se pudo enviar el correo a " + destino.trim() + ": " + detalleError, ultimoError);
    }

    // Asíncrono: para confirmaciones y notificaciones que no deben romper la transacción de negocio.
    @Async("emailExecutor")
    public void enviarAsync(String destino, String asunto, String html) {
        try {
            enviar(destino, asunto, html);
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