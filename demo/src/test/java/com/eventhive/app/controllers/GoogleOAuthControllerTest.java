package com.eventhive.app.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.eventhive.app.exception.CorreoException;
import com.eventhive.app.service.GmailOAuthService;
import com.eventhive.app.utils.GmailClientProvider;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthControllerTest {

    @Mock
    private GmailOAuthService gmailOAuthService;

    @Mock
    private GmailClientProvider clientProvider;

    private GoogleOAuthController controller;

    @BeforeEach
    void setUp() {
        controller = new GoogleOAuthController(gmailOAuthService, clientProvider);
    }

    @Test
    void iniciarAutorizacionRedirigeAGoogle() {
        String urlEsperada = "https://accounts.google.com/o/oauth2/v2/auth?client_id=test-id&redirect_uri=http://localhost:8080/oauth2/callback/google&response_type=code&scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fgmail.send&access_type=offline&prompt=consent";
        when(gmailOAuthService.construirUrlAutorizacion()).thenReturn(urlEsperada);

        ResponseEntity<?> response = controller.iniciarAutorizacion();

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals(urlEsperada, response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }

    @Test
    void iniciarAutorizacionRetorna400SiFaltanCredenciales() {
        when(gmailOAuthService.construirUrlAutorizacion())
                .thenThrow(new CorreoException("GMAIL_CLIENT_ID no está configurado"));

        ResponseEntity<?> response = controller.iniciarAutorizacion();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Configuración Incompleta"));
    }

    @Test
    void callbackRetorna400SiGoogleDevuelveError() {
        ResponseEntity<String> response = controller.procesarCallback(null, "access_denied");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Autorización Cancelada"));
    }

    @Test
    void callbackRetorna400SiCodigoEstaVacio() {
        ResponseEntity<String> response = controller.procesarCallback("", null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Código no recibido"));
    }

    @Test
    void callbackExitosoMuestraInstruccionesYTokenParaRender() {
        when(gmailOAuthService.intercambiarCodigoPorRefreshToken("auth-code-123"))
                .thenReturn("1//mock-refresh-token-xyz");
        when(clientProvider.getSender()).thenReturn("eventhiveContacto@gmail.com");

        ResponseEntity<String> response = controller.procesarCallback("auth-code-123", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("¡Autorización Exitosa!"));
        assertTrue(response.getBody().contains("1//mock-refresh-token-xyz"));
        assertTrue(response.getBody().contains("GMAIL_REFRESH_TOKEN"));
    }

    @Test
    void consultarEstadoRetorna200CuandoOAuthEsValido() {
        when(clientProvider.getSender()).thenReturn("eventhiveContacto@gmail.com");
        when(clientProvider.probarConexionOAuth()).thenReturn(
                new GmailClientProvider.OAuthDiagnostico(
                        true, 200, "OK", "Token renovado exitosamente", "Todo en orden")
        );

        ResponseEntity<String> response = controller.consultarEstado();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Conexión OAuth 2.0 Verificada"));
        assertTrue(response.getBody().contains("eventhiveContacto@gmail.com"));
    }

    @Test
    void consultarEstadoRetorna503CuandoOAuthFalla() {
        when(clientProvider.getSender()).thenReturn("eventhiveContacto@gmail.com");
        when(clientProvider.probarConexionOAuth()).thenReturn(
                new GmailClientProvider.OAuthDiagnostico(
                        false, 400, "invalid_grant", "Token expirado o revocado", "Reautorizar")
        );

        ResponseEntity<String> response = controller.consultarEstado();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("invalid_grant"));
        assertTrue(response.getBody().contains("Token expirado o revocado"));
        assertTrue(response.getBody().contains("/oauth2/authorize/google"));
    }
}
