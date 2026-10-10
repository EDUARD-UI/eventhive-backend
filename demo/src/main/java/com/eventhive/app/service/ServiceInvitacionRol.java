package com.eventhive.app.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.dto.request.AceptarInvitacionRolRequest;
import com.eventhive.app.dto.response.InvitacionRolDTO;
import com.eventhive.app.dto.response.InvitacionRolEnviadaDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.InvitacionRol;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Rol;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.InvitacionRolRepository;
import com.eventhive.app.repository.RolesRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceInvitacionRol {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String[] CODIGOS_ADMIN = {"MODERADOR", "MARKETING"};

    private final InvitacionRolRepository invitaciones;
    private final UsuarioRepository usuarios;
    private final RolesRepository roles;
    private final AuthenticatedUserHelper authHelper;
    private final PasswordEncoder passwordEncoder;
    private final ServiceCorreo serviceCorreo;

    @Value("${eventhive.frontend-url:https://eventhive-ruby-zeta.vercel.app}")
    private String frontendUrl;

    @Transactional
    public void invitar(String correo, String rolDestino) {
        Usuario remitente = authHelper.usuarioAutenticado();
        String correoNormalizado = correo.trim().toLowerCase(Locale.ROOT);
        String rol = rolDestino.toUpperCase(Locale.ROOT);
        Organizacion organizacion = null;

        if (esRol(remitente, "ADMINISTRADOR")) {
            if (!contiene(CODIGOS_ADMIN, rol)) {
                throw new BusinessException("Administración solo puede invitar MODERADOR o MARKETING");
            }
        } else if (esRol(remitente, "REPRESENTANTE")) {
            if (!"OPERADOR".equals(rol)) {
                throw new BusinessException("Los representantes solo pueden invitar OPERADOR");
            }
            organizacion = remitente.getOrganizacion();
            if (organizacion == null || organizacion.getRepresentante() == null
                    || !organizacion.getRepresentante().getId().equals(remitente.getId())) {
                throw new BusinessException("Solo el representante de una organización puede invitar operadores");
            }
        } else {
            throw new BusinessException("No tienes permisos para invitar este rol");
        }

        usuarios.findByCorreoIgnoreCase(correoNormalizado).ifPresent(existente -> {
            if (existente.getRol() == null || !"CLIENTE".equals(existente.getRol().getNombre())
                    || existente.getOrganizacion() != null) {
                throw new BusinessException("El correo ya está asociado a una cuenta que no puede recibir esta invitación");
            }
        });
        if (invitaciones.existsByCorreoInvitadoIgnoreCaseAndFechaAceptacionIsNullAndFechaExpiracionAfter(
                correoNormalizado, LocalDateTime.now())) {
            throw new BusinessException("Ya existe una invitación pendiente para este correo");
        }

        roles.findByNombre(rol)
                .orElseThrow(() -> new ResourceNotFoundException("Rol " + rol + " no configurado"));

        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        InvitacionRol invitacion = new InvitacionRol();
        invitacion.setCorreoInvitado(correoNormalizado);
        invitacion.setRolDestino(rol);
        invitacion.setTokenHash(hash(token));
        invitacion.setFechaExpiracion(LocalDateTime.now().plusHours(24));
        invitacion.setInvitadoPor(remitente);
        invitacion.setOrganizacion(organizacion);
        invitaciones.save(invitacion);

        String link = frontendUrl.replaceAll("/+$", "") + "/aceptar-invitacion?token=" + token;
        serviceCorreo.enviarInvitacionRol(invitacion.getCorreoInvitado(), rol, link);
    }

    @Transactional(readOnly = true)
    public InvitacionRolDTO validar(String token) {
        InvitacionRol invitacion = obtenerValida(token);
        return new InvitacionRolDTO(invitacion.getCorreoInvitado(), invitacion.getRolDestino(),
                invitacion.getFechaExpiracion());
    }

    @Transactional(readOnly = true)
    public Page<InvitacionRolEnviadaDTO> listarEnviadas(Pageable pageable) {
        Usuario remitente = authHelper.usuarioAutenticado();
        if (!esRol(remitente, "ADMINISTRADOR") && !esRol(remitente, "REPRESENTANTE")) {
            throw new BusinessException("No tienes permiso para consultar invitaciones");
        }
        return invitaciones.findByInvitadoPorIdOrderByFechaInvitacionDesc(remitente.getId(), pageable)
                .map(invitacion -> new InvitacionRolEnviadaDTO(
                        invitacion.getId(), invitacion.getCorreoInvitado(), invitacion.getRolDestino(),
                        invitacion.getFechaInvitacion(), invitacion.getFechaExpiracion(),
                        invitacion.getFechaAceptacion()));
    }

    @Transactional
    public void aceptar(AceptarInvitacionRolRequest request) {
        InvitacionRol invitacion = obtenerValida(request.getToken());
        Rol rol = roles.findByNombre(invitacion.getRolDestino())
                .orElseThrow(() -> new ResourceNotFoundException("El rol invitado ya no está disponible"));

        Usuario usuario = usuarios.findByCorreoIgnoreCase(invitacion.getCorreoInvitado()).orElseGet(Usuario::new);
        if (usuario.getRol() != null && !"CLIENTE".equals(usuario.getRol().getNombre())) {
            throw new BusinessException("El correo ya está asociado a otro rol");
        }
        if (usuario.getOrganizacion() != null
                && (invitacion.getOrganizacion() == null
                || !usuario.getOrganizacion().getId().equals(invitacion.getOrganizacion().getId()))) {
            throw new BusinessException("La cuenta ya pertenece a una organización distinta");
        }
        usuario.setNombreCompleto(request.getNombre().trim());
        usuario.setCorreo(invitacion.getCorreoInvitado());
        usuario.setTelefono(request.getTelefono().trim());
        usuario.setClave(passwordEncoder.encode(request.getClave()));
        usuario.setActivo(true);
        usuario.setRol(rol);
        usuario.setOrganizacion(invitacion.getOrganizacion());
        usuarios.save(usuario);

        invitacion.setFechaAceptacion(LocalDateTime.now());
        invitaciones.save(invitacion);
    }

    private InvitacionRol obtenerValida(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("El enlace de invitación no es válido");
        }
        InvitacionRol invitacion = invitaciones.findByTokenHashAndFechaAceptacionIsNull(hash(token))
                .orElseThrow(() -> new ResourceNotFoundException("La invitación no existe o ya fue utilizada"));
        if (!invitacion.getFechaExpiracion().isAfter(LocalDateTime.now())) {
            throw new BusinessException("La invitación expiró; solicita un nuevo enlace");
        }
        return invitacion;
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }

    private boolean esRol(Usuario usuario, String nombre) {
        return usuario.getRol() != null && nombre.equals(usuario.getRol().getNombre());
    }

    private boolean contiene(String[] rolesPermitidos, String rol) {
        for (String permitido : rolesPermitidos) {
            if (permitido.equals(rol)) {
                return true;
            }
        }
        return false;
    }
}
