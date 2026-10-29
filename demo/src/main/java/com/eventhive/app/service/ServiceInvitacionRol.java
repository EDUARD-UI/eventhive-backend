package com.eventhive.app.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final com.eventhive.app.repository.OrganizacionRepository organizacionRepository;
    private final AuthenticatedUserHelper authHelper;
    private final PasswordEncoder passwordEncoder;
    private final ServiceCorreo serviceCorreo;
    private final ServiceNotification serviceNotification;

    @Value("${eventhive.frontend-url:https://eventhive-ruby-zeta.vercel.app}")
    private String frontendUrl;

    @Transactional
    public void invitar(String correo, String rolDestino) {
        if (correo == null || correo.isBlank() || !correo.contains("@")) {
            throw new BusinessException("El correo proporcionado es inválido");
        }
        if (rolDestino == null || rolDestino.isBlank()) {
            throw new BusinessException("El rol destino es obligatorio");
        }

        Usuario remitente = authHelper.usuarioAutenticado();
        String correoNormalizado = correo.trim().toLowerCase(Locale.ROOT);
        String rol = rolDestino.trim().toUpperCase(Locale.ROOT);
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
            if (organizacion == null) {
                organizacion = organizacionRepository.findByRepresentanteIdConRepresentante(remitente.getId())
                        .orElse(null);
            }
            if (organizacion == null || organizacion.getRepresentante() == null
                    || !organizacion.getRepresentante().getId().equals(remitente.getId())) {
                throw new BusinessException("Solo el representante de una organización puede invitar operadores");
            }
        } else {
            throw new AccessDeniedException("No tienes permisos para invitar este rol");
        }

        usuarios.findByCorreoIgnoreCase(correoNormalizado).ifPresent(existente -> {
            validarRolInvitado(existente, rol);
            if (existente.getOrganizacion() != null) {
                throw new BusinessException("La cuenta ya pertenece a una organización");
            }
        });

        roles.findByNombre(rol)
                .orElseThrow(() -> new ResourceNotFoundException("Rol " + rol + " no configurado"));

        if (invitaciones.existsByCorreoInvitadoIgnoreCaseAndFechaAceptacionIsNullAndFechaRechazoIsNullAndFechaExpiracionAfter(
                correoNormalizado, LocalDateTime.now())) {
            throw new BusinessException("Ya existe una invitación pendiente para este correo. Debe ser aceptada o rechazada antes de enviar una nueva.");
        }

        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        InvitacionRol invitacion = new InvitacionRol();
        invitacion.setCorreoInvitado(correoNormalizado);
        invitacion.setRolDestino(rol);
        invitacion.setTokenHash(hash(token));
        invitacion.setFechaInvitacion(LocalDateTime.now());
        invitacion.setFechaExpiracion(LocalDateTime.now().plusHours(24));
        invitacion.setInvitadoPor(remitente);
        invitacion.setOrganizacion(organizacion);

        invitaciones.save(invitacion);

        String baseUrl = (frontendUrl != null && !frontendUrl.isBlank())
                ? frontendUrl
                : "https://eventhive-ruby-zeta.vercel.app";
        String link = baseUrl.replaceAll("/+$", "") + "/aceptar-invitacion?token=" + token;
        serviceCorreo.enviarInvitacionRol(invitacion.getCorreoInvitado(), rol, link);
    }

    @Transactional(readOnly = true)
    public InvitacionRolDTO validar(String token) {
        InvitacionRol invitacion = obtenerValida(token);
        String orgNombre = invitacion.getOrganizacion() != null ? invitacion.getOrganizacion().getRazonSocial() : null;
        return new InvitacionRolDTO(invitacion.getCorreoInvitado(), invitacion.getRolDestino(),
                invitacion.getFechaExpiracion(), orgNombre, "PENDIENTE");
    }

    @Transactional(readOnly = true)
    public Page<InvitacionRolEnviadaDTO> listarEnviadas(Pageable pageable) {
        Usuario remitente = authHelper.usuarioAutenticado();
        if (esRol(remitente, "REPRESENTANTE")) {
            Organizacion org = remitente.getOrganizacion();
            if (org == null) {
                org = organizacionRepository.findByRepresentanteIdConRepresentante(remitente.getId()).orElse(null);
            }
            if (org == null) {
                throw new BusinessException("No tienes una organización para administrar");
            }
            return invitaciones.findByOrganizacionIdOrderByFechaInvitacionDesc(org.getId(), pageable)
                    .map(this::toEnviadaDTO);
        } else if (esRol(remitente, "ADMINISTRADOR")) {
            return invitaciones.findByInvitadoPorIdOrderByFechaInvitacionDesc(remitente.getId(), pageable)
                    .map(this::toEnviadaDTO);
        } else {
            throw new AccessDeniedException("No tienes permiso para consultar invitaciones");
        }
    }

    @Transactional(readOnly = true)
    public Page<InvitacionRolEnviadaDTO> listarInvitacionesDeMiOrganizacion(Pageable pageable) {
        Usuario remitente = authHelper.usuarioAutenticado();
        if (!esRol(remitente, "REPRESENTANTE")) {
            throw new AccessDeniedException("Solo los representantes pueden consultar invitaciones de organización");
        }
        Organizacion org = remitente.getOrganizacion();
        if (org == null) {
            org = organizacionRepository.findByRepresentanteIdConRepresentante(remitente.getId()).orElse(null);
        }
        if (org == null) {
            throw new BusinessException("No tienes una organización para administrar");
        }
        return invitaciones.findByOrganizacionIdOrderByFechaInvitacionDesc(org.getId(), pageable)
                .map(this::toEnviadaDTO);
    }

    @Transactional
    public void aceptar(AceptarInvitacionRolRequest request) {
        InvitacionRol invitacion = obtenerValida(request.getToken());
        Rol rol = roles.findByNombre(invitacion.getRolDestino())
                .orElseThrow(() -> new ResourceNotFoundException("El rol invitado ya no está disponible"));

        Usuario usuario = usuarios.findByCorreoIgnoreCase(invitacion.getCorreoInvitado()).orElse(null);
        boolean cuentaExistente = usuario != null;
        if (cuentaExistente) {
            validarRolInvitado(usuario, invitacion.getRolDestino());
        } else {
            validarDatosNuevaCuenta(request);
            usuario = new Usuario();
            usuario.setNombreCompleto(request.getNombre().trim());
            usuario.setCorreo(invitacion.getCorreoInvitado());
            usuario.setTelefono(request.getTelefono().trim());
            usuario.setClave(passwordEncoder.encode(request.getClave()));
            usuario.setActivo(true);
        }
        if (usuario.getOrganizacion() != null
                && (invitacion.getOrganizacion() == null
                || !usuario.getOrganizacion().getId().equals(invitacion.getOrganizacion().getId()))) {
            throw new BusinessException("La cuenta ya pertenece a una organización distinta");
        }
        usuario.setRol(rol);
        usuario.setOrganizacion(invitacion.getOrganizacion());
        if ("OPERADOR".equalsIgnoreCase(rol.getNombre())) {
            usuario.setPermisosEvento(new HashSet<>(Set.of(com.eventhive.app.enums.PermisoEvento.CHECK_IN)));
        } else {
            if (usuario.getPermisosEvento() == null) {
                usuario.setPermisosEvento(new HashSet<>());
            } else {
                usuario.getPermisosEvento().clear();
            }
        }
        usuarios.save(usuario);

        invitacion.setFechaAceptacion(LocalDateTime.now());
        invitaciones.save(invitacion);

        if (invitacion.getInvitadoPor() != null) {
            serviceNotification.notificarInvitacionAceptada(invitacion.getInvitadoPor(), usuario.getNombreCompleto());
        }
        if (invitacion.getOrganizacion() != null) {
            serviceNotification.notificarRolAsignado(
                    usuario, rol.getNombre(), invitacion.getOrganizacion().getRazonSocial());
        }
    }

    @Transactional
    public void rechazar(String token) {
        InvitacionRol invitacion = obtenerValida(token);
        if (invitacion.getFechaRechazo() != null) {
            throw new BusinessException("La invitación ya fue rechazada");
        }
        invitacion.setFechaRechazo(LocalDateTime.now());
        invitaciones.save(invitacion);
    }

    private InvitacionRol obtenerValida(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("El enlace de invitación no es válido");
        }
        InvitacionRol invitacion = invitaciones.findByTokenHashAndFechaAceptacionIsNull(hash(token))
                .orElseThrow(() -> new ResourceNotFoundException("La invitación no existe o ya fue utilizada"));
        if (invitacion.getFechaRechazo() != null) {
            throw new BusinessException("La invitación fue rechazada");
        }
        if (!invitacion.getFechaExpiracion().isAfter(LocalDateTime.now())) {
            throw new BusinessException("La invitación expiró; solicita un nuevo enlace");
        }
        return invitacion;
    }

    private InvitacionRolEnviadaDTO toEnviadaDTO(InvitacionRol invitacion) {
        String orgNombre = invitacion.getOrganizacion() != null ? invitacion.getOrganizacion().getRazonSocial() : null;
        return new InvitacionRolEnviadaDTO(
                invitacion.getId(),
                invitacion.getCorreoInvitado(),
                invitacion.getRolDestino(),
                invitacion.getFechaInvitacion(),
                invitacion.getFechaExpiracion(),
                invitacion.getFechaAceptacion(),
                invitacion.getFechaRechazo(),
                InvitacionRolEnviadaDTO.calcularEstado(invitacion.getFechaAceptacion(), invitacion.getFechaRechazo(), invitacion.getFechaExpiracion()),
                orgNombre
        );
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
        return usuario.getRol() != null && nombre.equalsIgnoreCase(usuario.getRol().getNombre());
    }

    private void validarRolInvitado(Usuario usuario, String rolDestino) {
        if (usuario.getRol() == null || "CLIENTE".equalsIgnoreCase(usuario.getRol().getNombre())) {
            return;
        }

        String rolActual = usuario.getRol().getNombre().toUpperCase(Locale.ROOT);
        if (rolActual.equals(rolDestino)) {
            throw new BusinessException("La persona ya tiene el rol " + rolActual);
        }

        String descripcionRol = switch (rolActual) {
            case "REPRESENTANTE" -> "representante";
            case "MODERADOR" -> "moderador";
            case "OPERADOR" -> "operador";
            case "ADMINISTRADOR" -> "administrador";
            case "MARKETING" -> "marketing";
            default -> rolActual;
        };
        throw new BusinessException("El correo ya tiene una cuenta con rol " + descripcionRol
                + "; solo se permiten invitaciones para clientes");
    }

    private void validarDatosNuevaCuenta(AceptarInvitacionRolRequest request) {
        if (!StringUtils.hasText(request.getNombre())
                || !StringUtils.hasText(request.getTelefono())
                || !StringUtils.hasText(request.getClave())) {
            throw new BusinessException("Para crear una cuenta nueva se requieren nombre, teléfono y contraseña");
        }
    }

    private boolean contiene(String[] rolesPermitidos, String rol) {
        for (String permitido : rolesPermitidos) {
            if (permitido.equalsIgnoreCase(rol)) {
                return true;
            }
        }
        return false;
    }
}
