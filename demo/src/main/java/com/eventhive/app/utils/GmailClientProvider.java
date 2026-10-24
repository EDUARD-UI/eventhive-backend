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

    public record OAuthDiagnostico(
            boolean exitoso,
            int httpStatus,
            String codigoOAuth,
            String descripcion,
            String recomendacion
    ) {}


    public static OAuthDiagnostico analizarErrorOAuth(Throwable t) {
        int status = 0;
        String codigoOAuth = "DESCONOCIDO";
        String descripcion = "Error al comunicarse con Google OAuth";
        String recomendacion = "Verifica la configuración y reautoriza en /oauth2/authorize/google";

        Throwable actual = t;
        String contenidoCrudo = "";
        while (actual != null) {
            String msg = actual.getMessage() != null ? actual.getMessage() : "";
            if (actual instanceof com.google.api.client.http.HttpResponseException hre) {
                status = hre.getStatusCode();
                if (hre.getContent() != null) {
                    contenidoCrudo = hre.getContent();
                }
            } else if (msg.contains("response code") || msg.contains("HTTP")) {
                var matcher = java.util.regex.Pattern.compile("\\b(4[0-9]{2}|5[0-9]{2})\\b").matcher(msg);
                if (matcher.find()) {
                    try {
                        status = Integer.parseInt(matcher.group(1));
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (StringUtils.hasText(msg)) {
                contenidoCrudo = contenidoCrudo + " " + msg;
            }
            actual = actual.getCause();
        }

        String textoAnalisis = contenidoCrudo.toLowerCase();

        if (textoAnalisis.contains("invalid_grant")) {
            status = status == 0 ? 400 : status;
            codigoOAuth = "invalid_grant";
            if (textoAnalisis.contains("expired") || textoAnalisis.contains("revoked")) {
                descripcion = "El refresh token fue revocado por el usuario o ha expirado (en proyectos en estado 'Testing' en Google Cloud expira a los 7 días).";
                recomendacion = "Vuelve a autorizar visitando /oauth2/authorize/google, copia el nuevo refresh token y actualízalo en el panel de Render.";
            } else {
                descripcion = "El refresh token no es válido para este cliente OAuth, está mal copiado o fue emitido para otro Client ID.";
                recomendacion = "Asegúrate de que GMAIL_CLIENT_ID y GMAIL_CLIENT_SECRET en Render coincidan con las credenciales de Google Cloud y genera un nuevo token en /oauth2/authorize/google.";
            }
        } else if (textoAnalisis.contains("invalid_client")) {
            status = status == 0 ? 401 : status;
            codigoOAuth = "invalid_client";
            descripcion = "Google no reconoce el Client ID o el Client Secret es incorrecto.";
            recomendacion = "Revisa GMAIL_CLIENT_ID y GMAIL_CLIENT_SECRET en el panel de Render. Asegúrate de que no contengan espacios en blanco o comillas accidentales.";
        } else if (textoAnalisis.contains("unauthorized_client")) {
            status = status == 0 ? 400 : status;
            codigoOAuth = "unauthorized_client";
            descripcion = "El cliente OAuth no está autorizado para este grant type o scope.";
            recomendacion = "Verifica en Google Cloud Console que el tipo de cliente sea 'Aplicación web' y que el ámbito https://www.googleapis.com/auth/gmail.send esté habilitado.";
        } else if (textoAnalisis.contains("redirect_uri_mismatch")) {
            status = status == 0 ? 400 : status;
            codigoOAuth = "redirect_uri_mismatch";
            descripcion = "La URI de redirección no coincide con la configurada en Google Cloud Console.";
            recomendacion = "Registra exactamente tu URL de callback en 'URIs de redireccionamiento autorizados' en Google Cloud Console.";
        } else if (status > 0) {
            codigoOAuth = "HTTP_" + status;
            descripcion = "Google respondió con código de estado HTTP " + status;
        }

        return new OAuthDiagnostico(false, status, codigoOAuth, descripcion, recomendacion);
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
