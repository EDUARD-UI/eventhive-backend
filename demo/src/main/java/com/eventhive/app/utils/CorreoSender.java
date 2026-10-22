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

    // Síncrono: propaga CorreoException si el servidor SMTP rechaza el correo tras los reintentos.
    public void enviar(String destino, String asunto, String html) {
        validarParametros(destino, asunto, html);

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
                log.warn("Fallo en intento {}/{} al enviar correo '{}' a {}: {}",
                        intento, intentos, sanitizarLog(asunto), sanitizarLog(destino), sanitizarLog(e.getMessage()));

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
        throw new CorreoException("No se pudo enviar el correo a " + destino.trim(), ultimoError);
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