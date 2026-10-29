package com.eventhive.app.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.dto.response.LoginResponseDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Rol;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.RolesRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.security.jwt.JwtUtils;
import com.eventhive.app.security.users.UsuarioPrincipal;

import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceAutenticacion {

    private static final int MAX_INTENTOS_FALLIDOS = 3;
    private static final long BLOQUEO_MINUTOS = 15;

    private final UsuarioRepository usuarioRepository;
    private final RolesRepository rolesRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final ObjectProvider<ServiceAutenticacion> self;

    @Transactional(noRollbackFor = {BadCredentialsException.class})
    public LoginResponseDTO autenticar(String correo, String clave) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(correo, clave));
        } catch (BadCredentialsException ex) {
            if (self != null && self.getIfAvailable() != null) {
                self.getIfAvailable().registrarIntentoFallido(correo);
            } else {
                registrarIntentoFallido(correo);
            }
            throw new BadCredentialsException("Credenciales incorrectas");
        } catch (LockedException ex) {
            throw ex;
        }

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        UsuarioPrincipal principal = (UsuarioPrincipal) userDetails;

        resetearIntentosFallidos(principal.getUsuario());

        String accessToken = jwtUtils.generarAccessToken(userDetails);
        String refreshToken = jwtUtils.generarRefreshToken(userDetails);

        Usuario u = principal.getUsuario();
        Long orgId = null;
        String orgNombre = null;
        Set<com.eventhive.app.enums.PermisoEvento> permisos = new HashSet<>();
        if (u.getOrganizacion() != null) {
            orgId = u.getOrganizacion().getId();
            orgNombre = u.getOrganizacion().getRazonSocial();
        }
        String rolNombre = u.getRol() != null ? u.getRol().getNombre() : "";
        if ("REPRESENTANTE".equalsIgnoreCase(rolNombre)) {
            permisos.addAll(Arrays.asList(com.eventhive.app.enums.PermisoEvento.values()));
        } else if ("OPERADOR".equalsIgnoreCase(rolNombre)) {
            permisos.add(com.eventhive.app.enums.PermisoEvento.CHECK_IN);
        }

        return LoginResponseDTO.builder()
                .id(principal.getUsuario().getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tipo("Bearer")
                .correo(principal.getUsername())
                .rol(principal.getUsuario().getRol().getNombre())
                .organizacionId(orgId)
                .organizacionNombre(orgNombre)
                .permisosEvento(permisos)
                .build();
    }

    // Emite un nuevo access token a partir de un refresh token válido (claim type=refresh)
    @Transactional(readOnly = true)
    public LoginResponseDTO refrescarToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BadCredentialsException("El refresh token es requerido");
        }

        String tokenLimpio = refreshToken.trim();

        if (jwtUtils.esTokenExpirado(tokenLimpio)) {
            String tipo = jwtUtils.getTipoDesdeToken(tokenLimpio);
            if (JwtUtils.TIPO_ACCESS.equals(tipo)) {
                log.warn("Petición a /refresh con Access Token expirado en lugar de Refresh Token.");
                throw new BadCredentialsException("El token proporcionado es un Access Token expirado. Se requiere un Refresh Token para renovar la sesión.");
            }
            log.info("Refresh token expirado. Solicitando nuevo inicio de sesión.");
            throw new BadCredentialsException("Refresh token expirado. Por favor, inicia sesión nuevamente.");
        }

        if (!jwtUtils.validarRefreshToken(tokenLimpio)) {
            String tipo = jwtUtils.getTipoDesdeToken(tokenLimpio);
            if (JwtUtils.TIPO_ACCESS.equals(tipo)) {
                log.warn("Petición a /refresh con Access Token vigente en lugar de Refresh Token.");
                throw new BadCredentialsException("El token proporcionado es un Access Token. El endpoint requiere un Refresh Token.");
            }
            log.warn("Petición a /refresh con token de firma o tipo inválido.");
            throw new BadCredentialsException("Refresh token inválido o corrupto. Por favor, inicia sesión nuevamente.");
        }

        String correo = jwtUtils.getCorreoDesdeToken(tokenLimpio);
        Usuario usuario = usuarioRepository.findByCorreoConContexto(correo)
                .or(() -> usuarioRepository.findByCorreo(correo))
                .orElseThrow(() -> new BadCredentialsException("Sesión inválida: usuario no encontrado con correo: " + correo));

        if (!usuario.isActivo()) {
            throw new BadCredentialsException("La cuenta no está activa. Contacta al administrador.");
        }

        if (usuario.getRol() == null || usuario.getRol().getNombre() == null
                || usuario.getRol().getNombre().isBlank()) {
            throw new BadCredentialsException("El usuario no tiene un rol válido asignado.");
        }

        if (usuario.getBloqueadoHasta() != null
                && usuario.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
            throw new LockedException(
                    "La cuenta está bloqueada temporalmente. Intenta de nuevo más tarde.");
        }

        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
        String nuevoAccessToken = jwtUtils.generarAccessToken(principal);

        Long orgId = null;
        String orgNombre = null;
        Set<com.eventhive.app.enums.PermisoEvento> permisos = new HashSet<>();
        if (usuario.getOrganizacion() != null) {
            orgId = usuario.getOrganizacion().getId();
            orgNombre = usuario.getOrganizacion().getRazonSocial();
        }
        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if ("REPRESENTANTE".equalsIgnoreCase(rolNombre)) {
            permisos.addAll(Arrays.asList(com.eventhive.app.enums.PermisoEvento.values()));
        } else if ("OPERADOR".equalsIgnoreCase(rolNombre)) {
            permisos.add(com.eventhive.app.enums.PermisoEvento.CHECK_IN);
        }

        return LoginResponseDTO.builder()
                .id(usuario.getId())
                .accessToken(nuevoAccessToken)
                .refreshToken(tokenLimpio)
                .tipo("Bearer")
                .correo(usuario.getCorreo())
                .rol(usuario.getRol().getNombre())
                .organizacionId(orgId)
                .organizacionNombre(orgNombre)
                .permisosEvento(permisos)
                .build();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIntentoFallido(String correo) {
        usuarioRepository.findByCorreo(correo)
                .ifPresent(usuario -> {
                    usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);

                    if (usuario.getIntentosFallidos() >= MAX_INTENTOS_FALLIDOS) {
                        usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(BLOQUEO_MINUTOS));
                    }

                    usuarioRepository.save(usuario);
                });
    }

    private void resetearIntentosFallidos(Usuario usuario) {
        if (usuario.getIntentosFallidos() == 0 && usuario.getBloqueadoHasta() == null) return;
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void registrarCliente(String nombre, String correo, String telefono, String clave) {
        validarRegistro(correo);
        Rol rol = rolesRepository.findByNombre("CLIENTE")
                .orElseThrow(() -> new BusinessException("Rol CLIENTE no existe"));
        usuarioRepository.save(crearUsuario(nombre, correo, telefono, clave, rol));
    }

    //METODOS AUXILIARES Y MAPEO
    private Usuario crearUsuario(String nombre, String correo,
                                 String telefono, String clave, Rol rol) {
        Usuario u = new Usuario();
        u.setNombreCompleto(nombre);
        u.setCorreo(correo);
        u.setTelefono(telefono);
        u.setClave(passwordEncoder.encode(clave));
        u.setActivo(true);
        u.setRol(rol);
        return u;
    }

    private void validarRegistro(String correo) {
        if (usuarioRepository.existsByCorreo(correo))
            throw new BusinessException("Correo ya registrado");
    }
}