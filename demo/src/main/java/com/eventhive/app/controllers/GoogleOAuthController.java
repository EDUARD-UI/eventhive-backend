package com.eventhive.app.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.eventhive.app.service.GmailOAuthService;
import com.eventhive.app.utils.GmailClientProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class GoogleOAuthController {

    private final GmailOAuthService gmailOAuthService;
    private final GmailClientProvider clientProvider;

    @GetMapping("/authorize/google")
    public ResponseEntity<?> iniciarAutorizacion() {
        try {
            String url = gmailOAuthService.construirUrlAutorizacion();
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, url)
                    .build();
        } catch (Exception e) {
            log.error("Error al generar la URL de autorización de Google: {}", e.getMessage());
            String htmlError = renderPagina("Configuración Incompleta",
                    "<div style='background:#fff0f0;border-left:4px solid #d93025;padding:16px;border-radius:4px;'>"
                    + "<h3 style='margin:0 0 8px;color:#d93025;'>No se puede iniciar la autorización OAuth</h3>"
                    + "<p style='margin:0;color:#333;'>" + sanitizarHtml(e.getMessage()) + "</p>"
                    + "</div>"
                    + "<p style='margin-top:16px;'>Asegúrate de configurar <code>GMAIL_CLIENT_ID</code> y <code>GMAIL_REDIRECT_URI</code> en tus variables de entorno.</p>");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_HTML)
                    .body(htmlError);
        }
    }

    @GetMapping(value = "/callback/google", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> procesarCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error) {

        if (StringUtils.hasText(error)) {
            log.warn("Google OAuth retornó un error durante el callback: {}", error);
            String htmlError = renderPagina("Autorización Cancelada",
                    "<div style='background:#fff0f0;border-left:4px solid #d93025;padding:16px;border-radius:4px;'>"
                    + "<h3 style='margin:0 0 8px;color:#d93025;'>Google denegó o canceló la autorización</h3>"
                    + "<p style='margin:0;color:#333;'>Motivo: " + sanitizarHtml(error) + "</p>"
                    + "</div>"
                    + "<p style='margin-top:20px;'><a href='/oauth2/authorize/google' style='background:#5b2a86;color:#fff;padding:10px 18px;text-decoration:none;border-radius:6px;font-weight:bold;'>Reintentar autorización</a></p>");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(htmlError);
        }

        if (!StringUtils.hasText(code)) {
            String htmlVacio = renderPagina("Código no recibido",
                    "<div style='background:#fffbe6;border-left:4px solid #faad14;padding:16px;border-radius:4px;'>"
                    + "<p style='margin:0;color:#333;'>No se recibió el parámetro <code>code</code> de Google.</p>"
                    + "</div>");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(htmlVacio);
        }

        try {
            String refreshToken = gmailOAuthService.intercambiarCodigoPorRefreshToken(code);
            String remitente = clientProvider.getSender();

            String htmlExito = renderPagina("¡Autorización Exitosa!",
                    "<div style='background:#edfbf3;border:1px solid #b7eb8f;padding:20px;border-radius:8px;margin-bottom:24px;'>"
                    + "<h2 style='margin:0 0 8px;color:#237804;font-size:20px;'>🎉 Conexión con Gmail API establecida</h2>"
                    + "<p style='margin:0;color:#333;'>La cuenta remitente <strong>" + sanitizarHtml(remitente) + "</strong> quedó vinculada en la instancia actual.</p>"
                    + "</div>"
                    + "<h3 style='margin:16px 0 8px;color:#1f1f1f;'>Paso final para Render (Persistencia permanente):</h3>"
                    + "<p style='color:#555;font-size:14px;line-height:1.6;'>Copia este <strong>Refresh Token</strong> y agrégalo en tu panel de Render para que la autenticación persista tras los reinicios y despliegues:</p>"
                    + "<div style='position:relative;background:#f5f5f7;border:1px solid #d9d9d9;border-radius:6px;padding:12px;margin:16px 0;'>"
                    + "<textarea id='rtText' readonly style='width:100%;height:75px;border:none;background:transparent;resize:none;font-family:monospace;font-size:13px;color:#222;outline:none;'>"
                    + sanitizarHtml(refreshToken)
                    + "</textarea>"
                    + "<button onclick='copiarToken()' style='margin-top:8px;background:#5b2a86;color:#fff;border:none;padding:8px 16px;border-radius:5px;cursor:pointer;font-weight:bold;font-size:13px;'>📋 Copiar Refresh Token</button>"
                    + "<span id='copiadoMsg' style='display:none;color:#237804;margin-left:12px;font-weight:bold;font-size:13px;'>✓ ¡Copiado al portapapeles!</span>"
                    + "</div>"
                    + "<div style='background:#fafafa;border:1px dashed #d9d9d9;border-radius:8px;padding:16px;margin-top:20px;font-size:13px;color:#555;line-height:1.6;'>"
                    + "<strong style='color:#333;'>Configuración en Render:</strong>"
                    + "<ol style='margin:8px 0 0;padding-left:20px;'>"
                    + "<li>Ve a <strong>Dashboard de Render</strong> &rarr; tu Web Service de EventHive Backend.</li>"
                    + "<li>Pestaña <strong>Environment</strong>.</li>"
                    + "<li>Agrega la variable con clave: <code>GMAIL_REFRESH_TOKEN</code></li>"
                    + "<li>Pega el valor copiado y pulsa <strong>Save Changes</strong>.</li>"
                    + "</ol>"
                    + "</div>"
                    + "<script>"
                    + "function copiarToken() {"
                    + "  const copyText = document.getElementById('rtText');"
                    + "  copyText.select();"
                    + "  navigator.clipboard.writeText(copyText.value);"
                    + "  document.getElementById('copiadoMsg').style.display = 'inline';"
                    + "  setTimeout(() => { document.getElementById('copiadoMsg').style.display = 'none'; }, 3000);"
                    + "}"
                    + "</script>");

            return ResponseEntity.ok(htmlExito);

        } catch (Exception e) {
            log.error("Fallo durante el intercambio de token en /oauth2/callback/google: {}", e.getMessage());
            String htmlError = renderPagina("Error al canjear token",
                    "<div style='background:#fff0f0;border-left:4px solid #d93025;padding:16px;border-radius:4px;'>"
                    + "<h3 style='margin:0 0 8px;color:#d93025;'>No se pudo obtener el Refresh Token</h3>"
                    + "<p style='margin:0;color:#333;'>" + sanitizarHtml(e.getMessage()) + "</p>"
                    + "</div>"
                    + "<p style='margin-top:16px;'>Verifica que <code>GMAIL_CLIENT_ID</code> y <code>GMAIL_CLIENT_SECRET</code> sean correctos y que la URI de redirección coincida exactamente con la registrada en Google Cloud Console.</p>"
                    + "<p><a href='/oauth2/authorize/google' style='background:#5b2a86;color:#fff;padding:10px 18px;text-decoration:none;border-radius:6px;font-weight:bold;'>Volver a intentar</a></p>");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(htmlError);
        }
    }

    private String renderPagina(String titulo, String cuerpo) {
        return "<!DOCTYPE html>"
                + "<html lang='es'>"
                + "<head>"
                + "<meta charset='UTF-8'/>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'/>"
                + "<title>" + sanitizarHtml(titulo) + " — EventHive</title>"
                + "</head>"
                + "<body style=\"font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;background:#f0f2f5;margin:0;padding:40px 16px;\">"
                + "<div style=\"max-width:640px;margin:0 auto;background:#ffffff;border-radius:12px;box-shadow:0 4px 16px rgba(0,0,0,0.08);padding:36px;\">"
                + "<div style=\"border-bottom:2px solid #5b2a86;padding-bottom:12px;margin-bottom:24px;\">"
                + "<h1 style=\"color:#5b2a86;margin:0;font-size:24px;\">EventHive &middot; Configuración Gmail API</h1>"
                + "</div>"
                + cuerpo
                + "</div>"
                + "</body>"
                + "</html>";
    }

    private String sanitizarHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
