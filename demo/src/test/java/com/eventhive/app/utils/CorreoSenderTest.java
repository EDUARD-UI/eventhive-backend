package com.eventhive.app.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.eventhive.app.exception.CorreoException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class CorreoSenderTest {

    @Mock
    private GmailClientProvider gmailClientProvider;

    @Mock
    private Gmail gmail;

    @Mock
    private Gmail.Users users;

    @Mock
    private Gmail.Users.Messages messages;

    @Mock
    private Gmail.Users.Messages.Send sendCall;

    private CorreoSender correoSender;

    @BeforeEach
    void configurar() {
        correoSender = new CorreoSender(gmailClientProvider);
        ReflectionTestUtils.setField(correoSender, "remitente", "eventhiveContacto@gmail.com");
        ReflectionTestUtils.setField(correoSender, "nombreRemitente", "EventHive");
        ReflectionTestUtils.setField(correoSender, "maxReintentos", 1);
    }

    @Test
    void enviaCorreoHtmlViaGmailApiAlDestinatarioConfigurado() throws Exception {
        when(gmailClientProvider.estaConfigurado()).thenReturn(true);
        when(gmailClientProvider.obtenerCliente()).thenReturn(gmail);
        when(gmail.users()).thenReturn(users);
        when(users.messages()).thenReturn(messages);
        when(messages.send(eq("me"), any(Message.class))).thenReturn(sendCall);

        Message respuestaExitosa = new Message().setId("msg-123456");
        when(sendCall.execute()).thenReturn(respuestaExitosa);

        correoSender.enviar("persona@example.com", "Invitación", "<p>Hola</p>");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messages).send(eq("me"), captor.capture());

        Message enviado = captor.getValue();
        assertNotNull(enviado);
        assertNotNull(enviado.getRaw());

        // Decodificar Base64 URL-safe y verificar contenido MIME
        byte[] bytesMime = Base64.getUrlDecoder().decode(enviado.getRaw());
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(bytesMime));

        assertEquals("persona@example.com", mimeMessage.getAllRecipients()[0].toString());
        assertEquals("Invitación", mimeMessage.getSubject());
        assertTrue(mimeMessage.getContentType().contains("text/html"));
        assertEquals("<p>Hola</p>", mimeMessage.getContent());
    }

    @Test
    void propagaErrorCuandoGmailApiFalla() throws Exception {
        when(gmailClientProvider.estaConfigurado()).thenReturn(true);
        when(gmailClientProvider.obtenerCliente()).thenReturn(gmail);
        when(gmail.users()).thenReturn(users);
        when(users.messages()).thenReturn(messages);
        when(messages.send(eq("me"), any(Message.class))).thenReturn(sendCall);
        when(sendCall.execute()).thenThrow(new IOException("Gmail API 500 Backend Error"));

        assertThrows(CorreoException.class,
                () -> correoSender.enviar("persona@example.com", "Invitación", "<p>Hola</p>"));
    }

    @Test
    void fallaCuandoCredencialesGmailEstanIncompletas() {
        when(gmailClientProvider.estaConfigurado()).thenReturn(false);

        assertThrows(CorreoException.class,
                () -> correoSender.enviar("persona@example.com", "Invitación", "<p>Hola</p>"));
    }
}
