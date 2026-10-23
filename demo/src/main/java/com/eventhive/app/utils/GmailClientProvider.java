package com.eventhive.app.utils;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.eventhive.app.exception.CorreoException;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GmailClientProvider {

    @Getter
    @Value("${gmail.client-id:${GMAIL_CLIENT_ID:}}")
    private String clientId;

    @Getter
    @Value("${gmail.client-secret:${GMAIL_CLIENT_SECRET:}}")
    private String clientSecret;

    @Getter
    @Value("${gmail.refresh-token:${GMAIL_REFRESH_TOKEN:}}")
    private volatile String refreshToken;

    @Getter
    @Value("${gmail.sender:${GMAIL_SENDER:eventhiveContacto@gmail.com}}")
    private String sender;

    @Getter
    @Value("${gmail.redirect-uri:${GMAIL_REDIRECT_URI:http://localhost:8080/oauth2/callback/google}}")
    private String redirectUri;

    @Value("${gmail.application-name:EventHive}")
    private String applicationName;

    private volatile Gmail cachedClient;

    public boolean estaConfigurado() {
        return StringUtils.hasText(clientId)
                && StringUtils.hasText(clientSecret)
                && StringUtils.hasText(refreshToken);
    }

    public synchronized void actualizarRefreshToken(String nuevoRefreshToken) {
        if (StringUtils.hasText(nuevoRefreshToken)) {
            this.refreshToken = nuevoRefreshToken.trim();
            this.cachedClient = null;
            log.info("Nuevo refresh token registrado en memoria para el remitente '{}'", sender);
        }
    }

    public Gmail obtenerCliente() {
        if (!estaConfigurado()) {
            throw new CorreoException("El servicio Gmail API no está configurado: faltan GMAIL_CLIENT_ID, GMAIL_CLIENT_SECRET o GMAIL_REFRESH_TOKEN");
        }

        Gmail client = this.cachedClient;
        if (client == null) {
            synchronized (this) {
                client = this.cachedClient;
                if (client == null) {
                    client = construirCliente();
                    this.cachedClient = client;
                }
            }
        }
        return client;
    }

    private Gmail construirCliente() {
        try {
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId.trim())
                    .setClientSecret(clientSecret.trim())
                    .setRefreshToken(refreshToken.trim())
                    .build();

            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            return new Gmail.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    requestInitializer
            ).setApplicationName(applicationName).build();
        } catch (GeneralSecurityException | IOException e) {
            log.error("Error al inicializar cliente Gmail API: {}", e.getMessage());
            throw new CorreoException("Error al inicializar el cliente oficial de Gmail API: " + e.getMessage(), e);
        }
    }
}
