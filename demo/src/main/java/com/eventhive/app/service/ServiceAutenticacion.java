package com.eventhive.app.service;

import com.eventhive.app.dto.response.LoginResponseDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.model.Rol;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.RolesRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.security.jwt.JwtUtils;
import com.eventhive.app.security.users.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    @Transactional
    public LoginResponseDTO autenticar(String correo, String clave) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(correo, clave));
        } catch (BadCredentialsException ex) {
            registrarIntentoFallido(correo);
            throw ex;
        } catch (LockedException ex) {
            throw new BusinessException(
                    "Cuenta bloqueada temporalmente por múltiples intentos fallidos. Intenta de nuevo en unos minutos.");
        }

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        UsuarioPrincipal principal = (UsuarioPrincipal) userDetails;

        resetearIntentosFallidos(principal.getUsuario());

        String accessToken = jwtUtils.generarAccessToken(userDetails);
        String refreshToken = jwtUtils.generarRefreshToken(userDetails);

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tipo("Bearer")
                .correo(principal.getUsername())
                .rol(principal.getUsuario().getRol().getNombre())
                .build();
    }

    // Emite un nuevo access token a partir de un refresh token válido (claim type=refresh)
    @Transactional(readOnly = true)
    public LoginResponseDTO refrescarToken(String refreshToken) {
        if (!jwtUtils.validarRefreshToken(refreshToken))
            throw new BusinessException("Refresh token inválido o expirado");

        String correo = jwtUtils.getCorreoDesdeToken(refreshToken);
        Usuario usuario = usuarioRepository.findByCorreo(correo);

        if (usuario == null)
            throw new BusinessException("Usuario no encontrado");

        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
        String nuevoAccessToken = jwtUtils.generarAccessToken(principal);

        return LoginResponseDTO.builder()
                .accessToken(nuevoAccessToken)
                .refreshToken(refreshToken)
                .tipo("Bearer")
                .correo(usuario.getCorreo())
                .rol(usuario.getRol().getNombre())
                .build();
    }

    private void registrarIntentoFallido(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo);
        if (usuario == null) return; // no revela si el correo existe

        usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);

        if (usuario.getIntentosFallidos() >= MAX_INTENTOS_FALLIDOS) {
            usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(BLOQUEO_MINUTOS));
        }

        usuarioRepository.save(usuario);
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

    @Transactional
    public void registrarOrganizacion(String nombre, String correo, String telefono, String clave) {
        validarRegistro(correo);
        Rol rol = rolesRepository.findByNombre("ORGANIZACION")
                .orElseThrow(() -> new BusinessException("Rol ORGANIZACION no existe"));
        usuarioRepository.save(crearUsuario(nombre, correo, telefono, clave, rol));
    }

    private Usuario crearUsuario(String nombre, String correo,
                                 String telefono, String clave, Rol rol) {
        Usuario u = new Usuario();
        u.setNombreCompleto(nombre);
        u.setCorreo(correo);
        u.setTelefono(telefono);
        u.setClave(passwordEncoder.encode(clave));
        u.setRol(rol);
        return u;
    }

    private void validarRegistro(String correo) {
        if (usuarioRepository.existsByCorreo(correo))
            throw new BusinessException("Correo ya registrado");
    }
}