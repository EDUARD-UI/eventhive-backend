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
import com.eventhive.app.enums.EstadoInvitacion;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.InvitacionRol;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Rol;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.InvitacionRolRepository;
import com.eventhive.app.repository.OrganizacionRepository;
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
    private final OrganizacionRepository organizacionRepository;
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
        String rolNombre = rolDestino.trim().toUpperCase(Locale.ROOT);
        Organizacion organizacion = null;

        if (esRol(remitente, "ADMINISTRADOR")) {
            if (!contiene(CODIGOS_ADMIN, rolNombre)) {
                throw new BusinessException("Administración solo puede invitar MODERADOR o MARKETING");
            }
        } else if (esRol(remitente, "REPRESENTANTE")) {
            if (!"OPERADOR".equals(rolNombre)) {
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
            validarRolInvitado(existente, rolNombre);
            if (existente.getOrganizacion() != null) {
                throw new BusinessException("La cuenta ya pertenece a una organización");
            }
        });

        Rol rolEntidad = roles.findByNombre(rolNombre)
                .orElseThrow(() -> new ResourceNotFoundException("Rol " + rolNombre + " no configurado"));

        if (invitaciones.existsByCorreoInvitadoIgnoreCaseAndFechaAceptacionIsNullAndFechaExpiracionAfter(
                correoNormalizado, LocalDateTime.now())) {
            throw new BusinessException("Ya existe una invitación pendiente para este correo. Debe ser aceptada antes de enviar una nueva.");
        }

        String token = generarToken();

        InvitacionRol invitacion = new InvitacionRol();
        invitacion.setCorreoInvitado(correoNormalizado);
        invitacion.setRolDestino(rolEntidad);
        invitacion.setTokenHash(hash(token));
        invitacion.setFechaInvitacion(LocalDateTime.now());
        invitacion.setFechaExpiracion(LocalDateTime.now().plusHours(24));
        invitacion.setEstado(EstadoInvitacion.PENDIENTE);
        invitacion.setInvitadoPor(remitente);
        invitacion.setOrganizacion(organizacion);

        invitaciones.save(invitacion);

        String link = construirLinkInvitacion(token);
        serviceCorreo.enviarInvitacionRol(invitacion.getCorreoInvitado(), rolEntidad.getNombre(), link);
    }

    @Transactional(readOnly = true)
    public InvitacionRolDTO validar(String token) {
        InvitacionRol invitacion = obtenerValida(token);
        String orgNombre = invitacion.getOrganizacion() != null ? invitacion.getOrganizacion().getRazonSocial() : null;
        return new InvitacionRolDTO(invitacion.getCorreoInvitado(), invitacion.getRolNombre(),
                invitacion.getFechaExpiracion(), orgNombre, invitacion.getEstado());
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
            return invitaciones.findActivasYExpiradasPorOrganizacion(org.getId(), pageable)
                    .map(this::toEnviadaDTO);
        } else if (esRol(remitente, "ADMINISTRADOR")) {
            return invitaciones.findActivasYExpiradasPorInvitador(remitente.getId(), pageable)
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
        return invitaciones.findActivasYExpiradasPorOrganizacion(org.getId(), pageable)
                .map(this::toEnviadaDTO);
    }

    @Transactional
    public void aceptar(AceptarInvitacionRolRequest request) {
        InvitacionRol invitacion = obtenerValida(request.getToken());
        Rol rol = invitacion.getRolDestino();
        if (rol == null) {
            throw new ResourceNotFoundException("El rol invitado ya no está disponible");
        }

        Usuario usuario = usuarios.findByCorreoIgnoreCase(invitacion.getCorreoInvitado()).orElse(null);
        boolean cuentaExistente = usuario != null;
        if (cuentaExistente) {
            validarRolInvitado(usuario, rol.getNombre());
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

        Usuario remitente = invitacion.getInvitadoPor();
        Organizacion orgInvitacion = invitacion.getOrganizacion();

        invitacion.setEstado(EstadoInvitacion.ACEPTADA);
        invitacion.setFechaAceptacion(LocalDateTime.now());
        invitaciones.delete(invitacion);

        if (remitente != null) {
            serviceNotification.notificarInvitacionAceptada(remitente, usuario.getNombreCompleto());
        }
        if (orgInvitacion != null) {
            serviceNotification.notificarRolAsignado(
                    usuario, rol.getNombre(), orgInvitacion.getRazonSocial());
        }
    }

    @Transactional
    public void eliminar(Long id) {
        InvitacionRol invitacion = invitaciones.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invitación no encontrada"));

        Usuario actual = authHelper.usuarioAutenticado();
        if (esRol(actual, "ADMINISTRADOR")) {
            invitaciones.delete(invitacion);
            return;
        }

        if (esRol(actual, "REPRESENTANTE")) {
            Organizacion org = actual.getOrganizacion();
            if (org == null) {
                org = organizacionRepository.findByRepresentanteIdConRepresentante(actual.getId()).orElse(null);
            }
            if (org != null && invitacion.getOrganizacion() != null && org.getId().equals(invitacion.getOrganizacion().getId())) {
                invitaciones.delete(invitacion);
                return;
            }
        }

        throw new AccessDeniedException("No estás autorizado para eliminar esta invitación");
    }

    @Transactional
    public void reenviar(Long id) {
        InvitacionRol invitacion = invitaciones.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invitación no encontrada"));

        Usuario actual = authHelper.usuarioAutenticado();
        validarPermisoGestion(actual, invitacion);

        if (!invitacion.isExpirada()) {
            throw new BusinessException("Solo se pueden reenviar invitaciones en estado EXPIRADA");
        }

        String nuevoToken = generarToken();
        invitacion.setTokenHash(hash(nuevoToken));
        invitacion.setFechaInvitacion(LocalDateTime.now());
        invitacion.setFechaExpiracion(LocalDateTime.now().plusHours(24));
        invitacion.setEstado(EstadoInvitacion.PENDIENTE);
        invitaciones.save(invitacion);

        String link = construirLinkInvitacion(nuevoToken);
        serviceCorreo.enviarInvitacionRol(invitacion.getCorreoInvitado(), invitacion.getRolNombre(), link);
    }

    @Transactional
    public void rechazar(String token) {
        InvitacionRol invitacion = obtenerValida(token);
        Usuario invitadoPor = invitacion.getInvitadoPor();
        String correo = invitacion.getCorreoInvitado();

        invitaciones.delete(invitacion);

        if (invitadoPor != null) {
            String nombreRechazante = usuarios.findByCorreoIgnoreCase(correo)
                    .map(Usuario::getNombreCompleto)
                    .filter(nombre -> nombre != null && !nombre.isBlank())
                    .orElse(correo);
            serviceNotification.notificarInvitacionRechazada(invitadoPor, nombreRechazante);
        }
    }

    private void validarPermisoGestion(Usuario actual, InvitacionRol invitacion) {
        if (esRol(actual, "ADMINISTRADOR")) {
            return;
        }
        if (esRol(actual, "REPRESENTANTE")) {
            Organizacion org = actual.getOrganizacion();
            if (org == null) {
                org = organizacionRepository.findByRepresentanteIdConRepresentante(actual.getId()).orElse(null);
            }
            if (org != null && invitacion.getOrganizacion() != null && org.getId().equals(invitacion.getOrganizacion().getId())) {
                return;
            }
        }
        throw new AccessDeniedException("No estás autorizado para gestionar esta invitación");
    }

    private InvitacionRol obtenerValida(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("El enlace de invitación no es válido");
        }
        InvitacionRol invitacion = invitaciones.findByTokenHashAndFechaAceptacionIsNull(hash(token))
                .orElseThrow(() -> new ResourceNotFoundException("La invitación no existe o ya fue utilizada"));
        if (invitacion.isExpirada()) {
            throw new BusinessException("La invitación expiró; solicita un nuevo enlace");
        }
        return invitacion;
    }

    private InvitacionRolEnviadaDTO toEnviadaDTO(InvitacionRol invitacion) {
        String orgNombre = invitacion.getOrganizacion() != null ? invitacion.getOrganizacion().getRazonSocial() : null;
        return new InvitacionRolEnviadaDTO(
                invitacion.getId(),
                invitacion.getCorreoInvitado(),
                invitacion.getRolNombre(),
                invitacion.getFechaInvitacion(),
                invitacion.getFechaEnvio(),
                invitacion.getFechaExpiracion(),
                invitacion.getFechaAceptacion(),
                invitacion.getEstado(),
                orgNombre
        );
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String construirLinkInvitacion(String token) {
        String baseUrl = (frontendUrl != null && !frontendUrl.isBlank())
                ? frontendUrl
                : "https://eventhive-ruby-zeta.vercel.app";
        return baseUrl.replaceAll("/+$", "") + "/aceptar-invitacion?token=" + token;
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
