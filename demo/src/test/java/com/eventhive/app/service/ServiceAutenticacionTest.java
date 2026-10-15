package com.eventhive.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.eventhive.app.model.Rol;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.RolesRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.security.jwt.JwtUtils;
import com.eventhive.app.security.users.UsuarioPrincipal;

@ExtendWith(MockitoExtension.class)
class ServiceAutenticacionTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolesRepository rolesRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private ObjectProvider<ServiceAutenticacion> self;

    @InjectMocks
    private ServiceAutenticacion serviceAutenticacion;

    @Test
    @DisplayName("Credenciales incorrectas incrementa intentos fallidos")
    void registrarIntentoFallidoIncrementaContador() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setCorreo("test@eventhive.com");
        usuario.setIntentosFallidos(1);

        when(usuarioRepository.findByCorreo("test@eventhive.com")).thenReturn(Optional.of(usuario));

        serviceAutenticacion.registrarIntentoFallido("test@eventhive.com");

        assertEquals(2, usuario.getIntentosFallidos());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("Tercer intento fallido bloquea la cuenta por 15 minutos")
    void registrarIntentoFallidoBloqueaAlTercerIntento() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setCorreo("test@eventhive.com");
        usuario.setIntentosFallidos(2);

        when(usuarioRepository.findByCorreo("test@eventhive.com")).thenReturn(Optional.of(usuario));

        serviceAutenticacion.registrarIntentoFallido("test@eventhive.com");

        assertEquals(3, usuario.getIntentosFallidos());
        assertNotNull(usuario.getBloqueadoHasta());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("Cuenta bloqueada lanza LockedException directamente")
    void autenticarConCuentaBloqueadaPropagaLockedException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new LockedException("Cuenta bloqueada"));

        assertThrows(LockedException.class, () ->
                serviceAutenticacion.autenticar("test@eventhive.com", "clave123"));
    }

    @Test
    @DisplayName("Autenticación exitosa resetea intentos fallidos")
    void autenticacionExitosaReseteaIntentos() {
        Rol rol = new Rol();
        rol.setNombre("CLIENTE");

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setCorreo("test@eventhive.com");
        usuario.setClave("encoded");
        usuario.setRol(rol);
        usuario.setIntentosFallidos(2);
        usuario.setBloqueadoHasta(LocalDateTime.now().minusMinutes(5));

        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
        var authResult = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authResult);
        when(jwtUtils.generarAccessToken(any())).thenReturn("access.jwt.token");
        when(jwtUtils.generarRefreshToken(any())).thenReturn("refresh.jwt.token");

        var response = serviceAutenticacion.autenticar("test@eventhive.com", "clave123");

        assertNotNull(response);
        assertEquals("test@eventhive.com", response.getCorreo());
        assertEquals("CLIENTE", response.getRol());
        assertEquals(0, usuario.getIntentosFallidos());
        verify(usuarioRepository).save(usuario);
    }
}
