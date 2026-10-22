package com.eventhive.app.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.eventhive.app.exception.CorreoException;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class CorreoSenderTest {

    @Mock
    private JavaMailSender mailSender;

    private CorreoSender correoSender;

    @BeforeEach
    void configurar() {
        correoSender = new CorreoSender(mailSender);
        ReflectionTestUtils.setField(correoSender, "remitente", "no-reply@example.com");
        ReflectionTestUtils.setField(correoSender, "nombreRemitente", "EventHive");
        ReflectionTestUtils.setField(correoSender, "mailUsername", "test-user@eventhive.live");
        ReflectionTestUtils.setField(correoSender, "mailPassword", "test-smtp-password");
        ReflectionTestUtils.setField(correoSender, "maxReintentos", 1);
    }

    @Test
    void enviaCorreoHtmlAlDestinatarioConfigurado() throws Exception {
        MimeMessage mensaje = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mensaje);

        correoSender.enviar("persona@example.com", "Invitación", "<p>Hola</p>");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage enviado = captor.getValue();
        enviado.saveChanges();
        assertEquals("persona@example.com", enviado.getAllRecipients()[0].toString());
        assertEquals("Invitación", enviado.getSubject());
        assertTrue(enviado.getContentType().contains("text/html"));
        assertEquals("<p>Hola</p>", enviado.getContent());
    }

    @Test
    void propagaErrorCuandoElProveedorSmtpFalla() throws Exception {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("SMTP no disponible")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(CorreoException.class,
                () -> correoSender.enviar("persona@example.com", "Invitación", "<p>Hola</p>"));
    }

    @Test
    void fallaCuandoCredencialesSmtpEstanVacias() {
        ReflectionTestUtils.setField(correoSender, "mailUsername", "");
        assertThrows(CorreoException.class,
                () -> correoSender.enviar("persona@example.com", "Invitación", "<p>Hola</p>"));
    }
}
