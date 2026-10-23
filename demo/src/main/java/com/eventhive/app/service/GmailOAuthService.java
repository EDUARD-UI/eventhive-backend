package com.eventhive.app.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.eventhive.app.exception.CorreoException;
import com.eventhive.app.utils.GmailClientProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailOAuthService {

    public static final String GMAIL_SEND_SCOPE = "https://www.googleapis.com/auth/gmail.send";
    private static final String GOOGLE_AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";

    private final GmailClientProvider clientProvider;

    public String construirUrlAutorizacion() {
        String clientId = clientProvider.getClientId();
        String redirectUri = clientProvider.getRedirectUri();

        if (!StringUtils.hasText(clientId)) {
            throw new CorreoException("GMAIL_CLIENT_ID no está configurado en las variables de entorno");
        }
        if (!StringUtils.hasText(redirectUri)) {
            throw new CorreoException("GMAIL_REDIRECT_URI no está configurado en las variables de entorno");
        }

        return GOOGLE_AUTH_ENDPOINT
                + "?client_id=" + URLEncoder.encode(clientId.trim(), StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri.trim(), StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode(GMAIL_SEND_SCOPE, StandardCharsets.UTF_8)
                + "&access_type=offline"
                + "&prompt=consent";
    }

    public String intercambiarCodigoPorRefreshToken(String code) {
        if (!StringUtils.hasText(code)) {
            throw new CorreoException("El código de autorización devuelto por Google está vacío");
        }

        String clientId = clientProvider.getClientId();
        String clientSecret = clientProvider.getClientSecret();
        String redirectUri = clientProvider.getRedirectUri();

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new CorreoException("Faltan GMAIL_CLIENT_ID o GMAIL_CLIENT_SECRET para intercambiar el código");
        }

        try {
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    GOOGLE_TOKEN_ENDPOINT,
                    clientId.trim(),
                    clientSecret.trim(),
                    code.trim(),
                    redirectUri.trim()
            ).execute();

            String refreshToken = tokenResponse.getRefreshToken();
            if (StringUtils.hasText(refreshToken)) {
                clientProvider.actualizarRefreshToken(refreshToken);
                log.info("Intercambio de código OAuth 2.0 completado exitosamente");
                return refreshToken.trim();
            } else {
                // Si Google no devuelve refresh_token (caso donde el usuario ya concedió acceso previo sin consent prompt),
                // verificamos si ya teníamos uno cargado.
                String actual = clientProvider.getRefreshToken();
                if (StringUtils.hasText(actual)) {
                    log.info("Google reutilizó el consentimiento previo; se conserva el refresh token configurado.");
                    return actual;
                }
                throw new CorreoException("Google no devolvió un refresh token. Vuelve a iniciar la autorización asegurándote de usar prompt=consent.");
            }
        } catch (GeneralSecurityException | IOException e) {
            log.error("Fallo al intercambiar el código de autorización de Google: {}", e.getMessage());
            throw new CorreoException("Error al comunicarse con Google OAuth token endpoint: " + e.getMessage(), e);
        }
    }
}
