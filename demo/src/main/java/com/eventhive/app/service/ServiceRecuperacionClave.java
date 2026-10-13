package com.eventhive.app.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.model.TokenRecuperacionClave;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.TokenRecuperacionClaveRepository;
import com.eventhive.app.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceRecuperacionClave {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long MINUTOS_VIGENCIA = 30;

    private final UsuarioRepository usuarios;
    private final TokenRecuperacionClaveRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final ServiceCorreo serviceCorreo;

    @Value("${eventhive.frontend-url:https://eventhive-ruby-zeta.vercel.app}")
    private String frontendUrl;

    // Nunca revela si el correo existe: responde igual en ambos casos.
    @Transactional
    public void solicitar(String correo) {
        usuarios.findByCorreoIgnoreCase(correo.trim())
                .filter(Usuario::isActivo)
                .ifPresent(this::emitirToken);
    }

    @Transactional
    public void restablecer(String token, String nuevaClave) {
        TokenRecuperacionClave registro = tokens.findVigentePorHash(hash(token))
                .filter(t -> t.getFechaExpiracion().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new BusinessException("El enlace es inválido o ya expiró"));

        if (tokens.marcarUsado(registro.getId(), LocalDateTime.now()) == 0) {
            throw new BusinessException("El enlace es inválido o ya expiró");
        }

        Usuario usuario = registro.getUsuario();
        usuario.setClave(passwordEncoder.encode(nuevaClave));
        usuario.setIntentosFallidos(0);      // desbloquea la cuenta si estaba bloqueada
        usuario.setBloqueadoHasta(null);
    }

    // ---------- auxiliares ----------

    private void emitirToken(Usuario usuario) {
        tokens.invalidarPendientes(usuario.getId(), LocalDateTime.now());

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        TokenRecuperacionClave registro = new TokenRecuperacionClave();
        registro.setUsuario(usuario);
        registro.setTokenHash(hash(token));
        registro.setFechaExpiracion(LocalDateTime.now().plusMinutes(MINUTOS_VIGENCIA));
        tokens.save(registro);

        String enlace = frontendUrl.replaceAll("/+$", "") + "/restablecer-clave?token=" + token;
        serviceCorreo.enviarRecuperarClave(usuario, enlace, MINUTOS_VIGENCIA);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }
}