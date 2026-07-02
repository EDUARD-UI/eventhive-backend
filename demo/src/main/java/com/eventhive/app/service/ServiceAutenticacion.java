package com.eventhive.app.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.dto.LoginResponseDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.model.Rol;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.RolesRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.security.jwt.JwtUtils;
import com.eventhive.app.security.users.UsuarioPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceAutenticacion {

    private final UsuarioRepository     usuarioRepository;
    private final RolesRepository       rolesRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder       passwordEncoder;
    private final JwtUtils              jwtUtils;

    public LoginResponseDTO autenticar(String correo, String clave) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(correo, clave));

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        UsuarioPrincipal principal = (UsuarioPrincipal) userDetails;

        String accessToken  = jwtUtils.generarAccessToken(userDetails);
        String refreshToken = jwtUtils.generarRefreshToken(userDetails);

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tipo("Bearer")
                .correo(principal.getUsername())
                .rol(principal.getUsuario().getRol().getNombre())
                .build();
    }

    @Transactional
    public void registrarCliente(String nombre, String correo, String telefono, String clave) {
        validarRegistro(correo);
        Rol rol = rolesRepository.findByNombre("CLIENTE")
                .orElseThrow(() -> new BusinessException("Rol CLIENTE no existe"));
        usuarioRepository.save(crearUsuario(nombre, correo, telefono, clave, rol));
    }

    @Transactional
    public void registrarOrganizador(String nombre, String correo, String telefono, String clave) {
        validarRegistro(correo);
        Rol rol = rolesRepository.findByNombre("ORGANIZADOR")
                .orElseThrow(() -> new BusinessException("Rol ORGANIZADOR no existe"));
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
        u.setInsigniaVerificacion(false);
        return u;
    }

    private void validarRegistro(String correo) {
        if (usuarioRepository.existsByCorreo(correo))
            throw new BusinessException("Correo ya registrado");
    }
}