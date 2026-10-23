package com.eventhive.app.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventhive.app.exception.CorreoException;
import com.eventhive.app.utils.GmailClientProvider;

@ExtendWith(MockitoExtension.class)
class GmailOAuthServiceTest {

    @Mock
    private GmailClientProvider clientProvider;

    private GmailOAuthService oauthService;

    @BeforeEach
    void setUp() {
        oauthService = new GmailOAuthService(clientProvider);
    }

    @Test
    void construirUrlAutorizacionGeneraUrlCompletaConParametrosRequeridos() {
        when(clientProvider.getClientId()).thenReturn("test-client-id.apps.googleusercontent.com");
        when(clientProvider.getRedirectUri()).thenReturn("https://api.eventhive.live/oauth2/callback/google");

        String url = oauthService.construirUrlAutorizacion();

        assertNotNull(url);
        assertTrue(url.startsWith("https://accounts.google.com/o/oauth2/v2/auth?"));
        assertTrue(url.contains("client_id=test-client-id.apps.googleusercontent.com"));
        assertTrue(url.contains("redirect_uri=https%3A%2F%2Fapi.eventhive.live%2Foauth2%2Fcallback%2Fgoogle"));
        assertTrue(url.contains("scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fgmail.send"));
        assertTrue(url.contains("access_type=offline"));
        assertTrue(url.contains("prompt=consent"));
    }

    @Test
    void construirUrlAutorizacionLanzaExcepcionSiFaltaClientId() {
        when(clientProvider.getClientId()).thenReturn("");

        assertThrows(CorreoException.class, () -> oauthService.construirUrlAutorizacion());
    }

    @Test
    void intercambiarCodigoLanzaExcepcionSiCodigoEsVacio() {
        assertThrows(CorreoException.class, () -> oauthService.intercambiarCodigoPorRefreshToken("  "));
    }
}
