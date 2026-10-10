package com.eventhive.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.eventhive.app.dto.request.AceptarInvitacionRolRequest;
import com.eventhive.app.model.InvitacionRol;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Rol;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.InvitacionRolRepository;
import com.eventhive.app.repository.RolesRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

@ExtendWith(MockitoExtension.class)
class ServiceInvitacionRolTest {

    @Mock private InvitacionRolRepository invitaciones;
    @Mock private UsuarioRepository usuarios;
    @Mock private RolesRepository roles;
    @Mock private AuthenticatedUserHelper authHelper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ServiceCorreo serviceCorreo;
    @InjectMocks private ServiceInvitacionRol service;

    @Test
    void invitacionDeOperadorVenceEn24HorasYActualizaCuentaClienteAlAceptar() throws Exception {
        ReflectionTestUtils.setField(service, "frontendUrl", "https://eventhive.example");
        Usuario representante = usuario(2L, "REPRESENTANTE");
        Organizacion organizacion = new Organizacion();
        organizacion.setId(9L);
        organizacion.setRepresentante(representante);
        representante.setOrganizacion(organizacion);

        Usuario cliente = usuario(3L, "CLIENTE");
        Rol rolOperador = rol("OPERADOR");
        when(authHelper.usuarioAutenticado()).thenReturn(representante);
        when(usuarios.findByCorreoIgnoreCase("cliente@example.com")).thenReturn(Optional.of(cliente));
        when(roles.findByNombre("OPERADOR")).thenReturn(Optional.of(rolOperador));
        when(invitaciones.save(any(InvitacionRol.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("passwordSeguro")).thenReturn("hash-password");

        service.invitar("Cliente@Example.com", "OPERADOR");

        ArgumentCaptor<InvitacionRol> invitationCaptor = ArgumentCaptor.forClass(InvitacionRol.class);
        verify(invitaciones).save(invitationCaptor.capture());
        InvitacionRol invitacion = invitationCaptor.getValue();
        assertEquals("cliente@example.com", invitacion.getCorreoInvitado());
        assertEquals("OPERADOR", invitacion.getRolDestino());
        assertEquals(organizacion, invitacion.getOrganizacion());
        assertTrue(invitacion.getFechaExpiracion().isAfter(LocalDateTime.now().plusHours(23)));
        assertTrue(invitacion.getFechaExpiracion().isBefore(LocalDateTime.now().plusHours(25)));

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(serviceCorreo).enviarInvitacionRol(eq("cliente@example.com"), eq("OPERADOR"), linkCaptor.capture());
        String token = URI.create(linkCaptor.getValue()).getQuery().substring("token=".length());
        String hash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        assertNotEquals(token, invitacion.getTokenHash());
        when(invitaciones.findByTokenHashAndFechaAceptacionIsNull(hash)).thenReturn(Optional.of(invitacion));

        AceptarInvitacionRolRequest aceptar = new AceptarInvitacionRolRequest();
        aceptar.setToken(token);
        aceptar.setNombre("Cliente invitado");
        aceptar.setTelefono("+573001234567");
        aceptar.setClave("passwordSeguro");
        service.aceptar(aceptar);

        assertEquals("OPERADOR", cliente.getRol().getNombre());
        assertEquals(organizacion, cliente.getOrganizacion());
        assertEquals("hash-password", cliente.getClave());
        assertTrue(invitacion.getFechaAceptacion() != null);
    }

    private Usuario usuario(Long id, String nombreRol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setCorreo("cliente@example.com");
        Rol rol = rol(nombreRol);
        usuario.setRol(rol);
        return usuario;
    }

    private Rol rol(String nombre) {
        Rol rol = new Rol();
        rol.setNombre(nombre);
        return rol;
    }
}
