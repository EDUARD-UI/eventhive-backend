package com.eventhive.app.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.eventhive.app.exception.CorreoException;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CorreoSender {

    private final JavaMailSender mailSender;

    @Value("${eventhive.mail.remitente:no-reply@eventhive.local}")
    private String remitente;

    @Value("${eventhive.mail.nombre-remitente:EventHive}")
    private String nombreRemitente;

    // Síncrono: propaga CorreoException (la operación depende del correo).
    public void enviar(String destino, String asunto, String html) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setFrom(remitente, nombreRemitente);
            helper.setTo(destino);
            helper.setSubject(asunto);
            helper.setText(html, true);
            mailSender.send(mensaje);
            log.info("Correo '{}' enviado a {}", asunto, destino);
        } catch (Exception e) {
            throw new CorreoException("No se pudo enviar el correo a " + destino, e);
        }
    }

    // Asíncrono: nunca rompe el flujo de negocio, solo deja log.
    @Async("emailExecutor")
    public void enviarAsync(String destino, String asunto, String html) {
        try {
            enviar(destino, asunto, html);
        } catch (CorreoException e) {
            log.error("Error enviando '{}' a {}: {}", asunto, destino, e.getMessage());
        }
    }
}