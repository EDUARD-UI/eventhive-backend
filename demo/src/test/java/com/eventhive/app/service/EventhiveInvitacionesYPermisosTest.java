package com.eventhive.app.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.eventhive.app.enums.PermisoEvento;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.InvitacionRol;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Rol;
import com.eventhive.app.model.Tiquete;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.InvitacionRolRepository;
import com.eventhive.app.repository.OrganizacionRepository;
import com.eventhive.app.repository.RolesRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

@ExtendWith(MockitoExtension.class)
class EventhiveInvitacionesYPermisosTest {

    @Mock
    private InvitacionRolRepository invitaciones;
    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private RolesRepository roles;
    @Mock
    private OrganizacionRepository organizacionRepository;
    @Mock
    private AuthenticatedUserHelper authHelper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ServiceCorreo serviceCorreo;
    @Mock
    private ServiceNotification serviceNotification;

    @InjectMocks
    private ServiceInvitacionRol serviceInvitacion;

    @InjectMocks
    private ServiceEvento serviceEvento;

    @InjectMocks
    private ServiceBoletos serviceBoletos;

    @Test
    @DisplayName("Administrador puede invitar MODERADOR o MARKETING exitosamente")
    void adminPuedeInvitarModeradorOMarketing() {
        Usuario admin = usuario(1L, "admin@eventhive.com", "ADMINISTRADOR", null);
        Rol rolModerador = rol("MODERADOR");

        when(authHelper.usuarioAutenticado()).thenReturn(admin);
        when(usuarios.findByCorreoIgnoreCase("mod@eventhive.com")).thenReturn(Optional.empty());
        when(roles.findByNombre("MODERADOR")).thenReturn(Optional.of(rolModerador));
        when(invitaciones.save(any(InvitacionRol.class))).thenAnswer(i -> i.getArgument(0));

        serviceInvitacion.invitar("mod@eventhive.com", "MODERADOR");

        ArgumentCaptor<InvitacionRol> captor = ArgumentCaptor.forClass(InvitacionRol.class);
        verify(invitaciones).save(captor.capture());
        InvitacionRol guardada = captor.getValue();

        assertEquals("mod@eventhive.com", guardada.getCorreoInvitado());
        assertEquals("MODERADOR", guardada.getRolNombre());
        assertEquals(null, guardada.getOrganizacion());
        verify(serviceCorreo).enviarInvitacionRol(eq("mod@eventhive.com"), eq("MODERADOR"), any());
    }

    @Test
    @DisplayName("Administrador NO puede invitar a rol ADMINISTRADOR ni OPERADOR")
    void adminNoPuedeInvitarRolNoPermitido() {
        Usuario admin = usuario(1L, "admin@eventhive.com", "ADMINISTRADOR", null);
        when(authHelper.usuarioAutenticado()).thenReturn(admin);

        BusinessException ex1 = assertThrows(BusinessException.class,
                () -> serviceInvitacion.invitar("super@eventhive.com", "ADMINISTRADOR"));
        assertTrue(ex1.getMessage().contains("MODERADOR o MARKETING"));

        BusinessException ex2 = assertThrows(BusinessException.class,
                () -> serviceInvitacion.invitar("op@eventhive.com", "OPERADOR"));
        assertTrue(ex2.getMessage().contains("MODERADOR o MARKETING"));

        verify(invitaciones, never()).save(any());
    }

    @Test
    @DisplayName("Representante puede invitar OPERADOR para su propia organización")
    void representantePuedeInvitarOperador() {
        Organizacion org = organizacion(10L, "Eventos SAS");
        Usuario rep = usuario(2L, "rep@eventos.com", "REPRESENTANTE", org);
        org.setRepresentante(rep);
        Rol rolOperador = rol("OPERADOR");

        when(authHelper.usuarioAutenticado()).thenReturn(rep);
        when(usuarios.findByCorreoIgnoreCase("operador@eventos.com")).thenReturn(Optional.empty());
        when(roles.findByNombre("OPERADOR")).thenReturn(Optional.of(rolOperador));
        when(invitaciones.save(any(InvitacionRol.class))).thenAnswer(i -> i.getArgument(0));

        serviceInvitacion.invitar("operador@eventos.com", "OPERADOR");

        ArgumentCaptor<InvitacionRol> captor = ArgumentCaptor.forClass(InvitacionRol.class);
        verify(invitaciones).save(captor.capture());
        InvitacionRol guardada = captor.getValue();

        assertEquals("operador@eventos.com", guardada.getCorreoInvitado());
        assertEquals("OPERADOR", guardada.getRolNombre());
        assertNotNull(guardada.getOrganizacion());
        assertEquals(10L, guardada.getOrganizacion().getId());
        verify(serviceCorreo).enviarInvitacionRol(eq("operador@eventos.com"), eq("OPERADOR"), any());
    }

    @Test
    @DisplayName("Representante NO puede invitar roles de plataforma como MODERADOR")
    void representanteNoPuedeInvitarRolesPlataforma() {
        Organizacion org = organizacion(10L, "Eventos SAS");
        Usuario rep = usuario(2L, "rep@eventos.com", "REPRESENTANTE", org);
        org.setRepresentante(rep);

        when(authHelper.usuarioAutenticado()).thenReturn(rep);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> serviceInvitacion.invitar("alguien@eventos.com", "MODERADOR"));
        assertTrue(ex.getMessage().contains("solo pueden invitar OPERADOR"));
        verify(invitaciones, never()).save(any());
    }

    @Test
    @DisplayName("Invitación pendiente no puede reenviarse: debe ser aceptada o rechazada")
    void invitacionPendienteNoPuedeReenviarse() {
        Usuario admin = usuario(1L, "admin@eventhive.com", "ADMINISTRADOR", null);
        Rol rolMarketing = rol("MARKETING");

        when(authHelper.usuarioAutenticado()).thenReturn(admin);
        when(usuarios.findByCorreoIgnoreCase("mkt@eventhive.com")).thenReturn(Optional.empty());
        when(roles.findByNombre("MARKETING")).thenReturn(Optional.of(rolMarketing));
        when(invitaciones.existsByCorreoInvitadoIgnoreCaseAndFechaAceptacionIsNullAndFechaExpiracionAfter(
                eq("mkt@eventhive.com"), any())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> serviceInvitacion.invitar("mkt@eventhive.com", "MARKETING"));
        assertTrue(ex.getMessage().contains("pendiente"));
        verify(invitaciones, never()).save(any());
    }

    @Test
    @DisplayName("Rechazar invitación elimina el registro y no permite volver a usarla")
    void rechazarInvitacionEliminaRegistro() throws Exception {
        String token = "token12345678901234567890123456789012";
        String hash = hash(token);

        InvitacionRol invitacion = new InvitacionRol();
        invitacion.setTokenHash(hash);
        invitacion.setCorreoInvitado("user@test.com");
        invitacion.setRolDestino(rol("OPERADOR"));
        invitacion.setFechaExpiracion(LocalDateTime.now().plusHours(12));

        when(invitaciones.findByTokenHashAndFechaAceptacionIsNull(hash)).thenReturn(Optional.of(invitacion));

        serviceInvitacion.rechazar(token);

        verify(invitaciones).delete(invitacion);
    }

    @Test
    @DisplayName("Token expirado o ya aceptado no puede ser validado ni utilizado")
    void validacionTokensInvalidos() throws Exception {
        String token = "token12345678901234567890123456789012";
        String hash = hash(token);

        when(invitaciones.findByTokenHashAndFechaAceptacionIsNull(hash)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> serviceInvitacion.validar(token));

        InvitacionRol expirada = new InvitacionRol();
        expirada.setTokenHash(hash);
        expirada.setCorreoInvitado("user@test.com");
        expirada.setRolDestino(rol("MODERADOR"));
        expirada.setFechaExpiracion(LocalDateTime.now().minusHours(1));

        when(invitaciones.findByTokenHashAndFechaAceptacionIsNull(hash)).thenReturn(Optional.of(expirada));
        BusinessException ex = assertThrows(BusinessException.class, () -> serviceInvitacion.validar(token));
        assertTrue(ex.getMessage().contains("expiró"));
    }

    @Test
    @DisplayName("Operador u organización ajena recibe 403 AccessDeniedException al acceder a evento de otra org")
    void accesoEntreOrganizacionesArroja403() {
        Organizacion orgA = organizacion(1L, "Org Alpha");
        Organizacion orgB = organizacion(2L, "Org Beta");

        Usuario operadorOrgA = usuario(5L, "op@alpha.com", "OPERADOR", orgA);
        operadorOrgA.setPermisosEvento(Set.of(PermisoEvento.EDITAR_EVENTO));

        Evento eventoOrgB = new Evento();
        eventoOrgB.setId(99L);
        eventoOrgB.setTitulo("Concierto Org B");
        eventoOrgB.setOrganizacion(orgB);

        when(authHelper.usuarioAutenticado()).thenReturn(operadorOrgA);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> serviceEvento.verificarPermiso(eventoOrgB, PermisoEvento.EDITAR_EVENTO));
        assertTrue(ex.getMessage().contains("No autorizado para modificar o acceder a este evento"));
    }

    @Test
    @DisplayName("Operador no puede gestionar eventos (solo representante legal)")
    void operadorSinPermisoRecibe403() {
        Organizacion org = organizacion(1L, "Org Alpha");
        org.setRepresentante(usuario(99L, "rep@alpha.com", "REPRESENTANTE", org));

        Usuario operadorSinPermiso = usuario(5L, "op@alpha.com", "OPERADOR", org);

        Evento evento = new Evento();
        evento.setId(10L);
        evento.setOrganizacion(org);

        when(authHelper.usuarioAutenticado()).thenReturn(operadorSinPermiso);

        AccessDeniedException exEditar = assertThrows(AccessDeniedException.class,
                () -> serviceEvento.verificarPermiso(evento, PermisoEvento.EDITAR_EVENTO));
        assertTrue(exEditar.getMessage().contains("representante"));

        AccessDeniedException exCancelar = assertThrows(AccessDeniedException.class,
                () -> serviceEvento.verificarPermiso(evento, PermisoEvento.CANCELAR_EVENTO));
        assertTrue(exCancelar.getMessage().contains("representante"));
    }

    @Test
    @DisplayName("Check-in: usuario no operador ni representante recibe 403 AccessDeniedException")
    void checkInSinPermisoRecibe403() {
        Organizacion org = organizacion(1L, "Org Alpha");
        org.setRepresentante(usuario(99L, "rep@alpha.com", "REPRESENTANTE", org));

        Usuario cliente = usuario(5L, "cliente@alpha.com", "CLIENTE", org);

        Evento evento = new Evento();
        evento.setOrganizacion(org);
        Tiquete tiquete = new Tiquete();
        tiquete.setId(123L);
        tiquete.setEvento(evento);

        when(authHelper.usuarioAutenticado()).thenReturn(cliente);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> ReflectionTestUtils.invokeMethod(serviceBoletos, "validarPermisoCheckIn", tiquete));
        assertTrue(ex.getMessage().contains("check-in"));
    }

    @Test
    @DisplayName("PermisosOperadorRequest deserializa array directo y aliases con insensibilidad a mayúsculas/minúsculas")
    void permisosOperadorRequestDeserializacionFlexible() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        String jsonArray = "[\"crear_evento\", \"check-in\"]";
        com.eventhive.app.dto.request.PermisosOperadorRequest reqArray =
                mapper.readValue(jsonArray, com.eventhive.app.dto.request.PermisosOperadorRequest.class);
        assertNotNull(reqArray.getPermisos());
        assertTrue(reqArray.getPermisos().contains(PermisoEvento.CREAR_EVENTO));
        assertTrue(reqArray.getPermisos().contains(PermisoEvento.CHECK_IN));

        String jsonObj = "{\"permisosEvento\": [\"EDITAR_EVENTO\", \"CANCELAR_EVENTO\"]}";
        com.eventhive.app.dto.request.PermisosOperadorRequest reqObj =
                mapper.readValue(jsonObj, com.eventhive.app.dto.request.PermisosOperadorRequest.class);
        assertNotNull(reqObj.getPermisos());
        assertTrue(reqObj.getPermisos().contains(PermisoEvento.EDITAR_EVENTO));
        assertTrue(reqObj.getPermisos().contains(PermisoEvento.CANCELAR_EVENTO));
    }

    private Usuario usuario(Long id, String correo, String rolNombre, Organizacion org) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setCorreo(correo);
        u.setNombreCompleto("Usuario " + id);
        u.setRol(rol(rolNombre));
        u.setOrganizacion(org);
        return u;
    }

    private Rol rol(String nombre) {
        Rol r = new Rol();
        r.setNombre(nombre);
        return r;
    }

    private Organizacion organizacion(Long id, String razonSocial) {
        Organizacion o = new Organizacion();
        o.setId(id);
        o.setRazonSocial(razonSocial);
        return o;
    }

    private String hash(String token) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    }
}
