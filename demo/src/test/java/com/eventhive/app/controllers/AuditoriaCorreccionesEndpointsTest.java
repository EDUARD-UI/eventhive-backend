package com.eventhive.app.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.response.OrganizacionDTO;
import com.eventhive.app.dto.response.PlanPromocionDTO;
import com.eventhive.app.dto.response.SolicitudVerificacionDTO;
import com.eventhive.app.enums.EstadoOrganizacion;
import com.eventhive.app.enums.EstadoSolicitud;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.CorreoException;
import com.eventhive.app.exception.GlobalExceptionHandler;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Rol;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.repository.InvitacionRolRepository;
import com.eventhive.app.repository.OrganizacionRepository;
import com.eventhive.app.repository.RolesRepository;
import com.eventhive.app.repository.SolicitudVerificacionRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.service.ServiceCorreo;
import com.eventhive.app.service.ServiceInvitacionRol;
import com.eventhive.app.service.ServiceMarketing;
import com.eventhive.app.service.ServiceOrganizacion;
import com.eventhive.app.service.ServiceSolicitudVerificacion;
import com.eventhive.app.service.SupabaseStorageService;
import com.eventhive.app.utils.AuthenticatedUserHelper;

@ExtendWith(MockitoExtension.class)
class AuditoriaCorreccionesEndpointsTest {

    // Handlers
    private GlobalExceptionHandler exceptionHandler;

    // Promociones
    @Mock private ServiceMarketing serviceMarketing;
    private PlanPromocionApiController planPromocionController;

    // Verificación
    @Mock private ServiceSolicitudVerificacion serviceSolicitud;
    private VerificacionApiController verificacionController;

    // Organizaciones
    @Mock private OrganizacionRepository organizacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private SolicitudVerificacionRepository solicitudRepository;
    @Mock private SupabaseStorageService storageService;
    @Mock private AuthenticatedUserHelper authHelper;
    @InjectMocks private ServiceOrganizacion serviceOrganizacion;

    // Invitaciones
    @Mock private InvitacionRolRepository invitacionRolRepository;
    @Mock private RolesRepository rolesRepository;
    @Mock private ServiceCorreo serviceCorreo;
    @InjectMocks private ServiceInvitacionRol serviceInvitacionRol;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        planPromocionController = new PlanPromocionApiController(serviceMarketing);
        verificacionController = new VerificacionApiController(serviceSolicitud);
        org.springframework.test.util.ReflectionTestUtils.setField(serviceInvitacionRol, "frontendUrl", "https://eventhive.example");
    }

    // =========================================================
    // 1. MANEJO GLOBAL DE EXCEPCIONES: 404 para rutas inexistentes y 502 para CorreoException
    // =========================================================

    @Test
    @DisplayName("NoResourceFoundException devuelve HTTP 404 y no 500")
    void noResourceFoundExceptionRetorna404() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "api/promociones");
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNoResourceFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMensaje().contains("Recurso no encontrado"));
    }

    @Test
    @DisplayName("CorreoException devuelve HTTP 502 Bad Gateway con mensaje seguro")
    void correoExceptionRetorna502() {
        CorreoException ex = new CorreoException("401 Unauthorized in Gmail API", new RuntimeException("Gmail 401"));
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleCorreo(ex);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMensaje().contains("Gmail API"));
    }

    @Test
    @DisplayName("ResourceNotFoundException conserva HTTP 404")
    void resourceNotFoundRetorna404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Organización no encontrada");
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Organización no encontrada", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("AccessDeniedException conserva HTTP 403")
    void accessDeniedRetorna403() {
        AccessDeniedException ex = new AccessDeniedException("No tienes permiso");
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // =========================================================
    // 2. PROMOCIONES PAGINADAS: GET /api/promociones?page=0&size=1 y size=10
    // =========================================================

    @Test
    @DisplayName("GET /api/promociones responde paginado con éxito para size=1 y size=10")
    void listarPromocionesPaginadas() {
        PlanPromocionDTO plan1 = new PlanPromocionDTO(1L, "IMPULSO", "Impulso Digital",
                new BigDecimal("500000"), new BigDecimal("10"), false, true, "Pauta en redes", true);
        Pageable pageable1 = PageRequest.of(0, 1);

        when(serviceMarketing.listarPlanesDisponibles(pageable1))
                .thenReturn(new PageImpl<>(List.of(plan1), pageable1, 2));

        ResponseEntity<ApiResponse<PagedResponse<PlanPromocionDTO>>> resp1 =
                planPromocionController.listarPlanesPaginados(pageable1);

        assertEquals(HttpStatus.OK, resp1.getStatusCode());
        assertNotNull(resp1.getBody());
        assertEquals(1, resp1.getBody().getData().getContent().size());
        assertEquals(2, resp1.getBody().getData().getTotalElements());
        assertEquals("IMPULSO", resp1.getBody().getData().getContent().get(0).getCodigo());

        // GET /api/promociones/planes sigue respondiendo lista
        when(serviceMarketing.listarPlanesDisponibles()).thenReturn(List.of(plan1));
        ResponseEntity<ApiResponse<List<PlanPromocionDTO>>> respPlanes =
                planPromocionController.listarPlanesDisponibles();
        assertEquals(HttpStatus.OK, respPlanes.getStatusCode());
        assertEquals(1, respPlanes.getBody().getData().size());
    }

    // =========================================================
    // 3. PERFIL DE ORGANIZACIÓN: GET /api/organizaciones/mi-organizacion
    // =========================================================

    @Test
    @DisplayName("miOrganizacion obtiene los datos cuando el usuario tiene organizacion asociada")
    void miOrganizacionConOrganizacionDirecta() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNombreCompleto("Representante Demo");
        usuario.setCorreo("rep@eventos.com");

        Organizacion org = new Organizacion();
        org.setId(5L);
        org.setRazonSocial("Eventos SAS");
        org.setNit("900123456");
        org.setEstado(EstadoOrganizacion.APROBADA);
        org.setRepresentante(usuario);
        usuario.setOrganizacion(org);

        when(authHelper.usuarioAutenticado()).thenReturn(usuario);
        when(organizacionRepository.findByIdConRepresentante(5L)).thenReturn(Optional.of(org));
        when(eventoRepository.countActivosByOrganizacionId(5L)).thenReturn(3L);
        when(solicitudRepository.findFirstByRepresentanteId(10L)).thenReturn(Optional.empty());

        OrganizacionDTO dto = serviceOrganizacion.miOrganizacion();

        assertNotNull(dto);
        assertEquals(5L, dto.getId());
        assertEquals("Eventos SAS", dto.getRazonSocial());
        assertEquals("Representante Demo", dto.getRepresentante());
        assertEquals(3L, dto.getEventosActivos());
    }

    @Test
    @DisplayName("miOrganizacion encuentra la organizacion por representanteId si usuario.organizacion está nulo")
    void miOrganizacionResuelvePorRepresentanteIdSiFaltaEnlaceUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNombreCompleto("Representante Demo");
        usuario.setCorreo("rep@eventos.com");
        usuario.setOrganizacion(null); // Sin asociación en el usuario

        Organizacion org = new Organizacion();
        org.setId(7L);
        org.setRazonSocial("Org Recuperada");
        org.setNit("900888777");
        org.setEstado(EstadoOrganizacion.APROBADA);
        org.setRepresentante(usuario);

        when(authHelper.usuarioAutenticado()).thenReturn(usuario);
        when(organizacionRepository.findByRepresentanteIdConRepresentante(10L)).thenReturn(Optional.of(org));
        when(organizacionRepository.findByIdConRepresentante(7L)).thenReturn(Optional.of(org));
        when(eventoRepository.countActivosByOrganizacionId(7L)).thenReturn(0L);
        when(solicitudRepository.findFirstByRepresentanteId(10L)).thenReturn(Optional.empty());

        OrganizacionDTO dto = serviceOrganizacion.miOrganizacion();

        assertNotNull(dto);
        assertEquals(7L, dto.getId());
        assertEquals("Org Recuperada", dto.getRazonSocial());
    }

    @Test
    @DisplayName("miOrganizacion lanza ResourceNotFoundException (404) si no tiene organizacion")
    void miOrganizacionSinOrganizacionLanzaNotFound() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setOrganizacion(null);

        when(authHelper.usuarioAutenticado()).thenReturn(usuario);
        when(organizacionRepository.findByRepresentanteIdConRepresentante(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> serviceOrganizacion.miOrganizacion());
    }

    // =========================================================
    // 4. VERIFICACIÓN: GET /api/verificacion/mi-solicitud
    // =========================================================

    @Test
    @DisplayName("GET /api/verificacion/mi-solicitud responde 200 con la solicitud del usuario")
    void miSolicitudRespondeConSolicitudExistente() {
        SolicitudVerificacionDTO solicitudDTO = new SolicitudVerificacionDTO();
        solicitudDTO.setId(100L);
        solicitudDTO.setRazonSocial("Mi Empresa SAS");
        solicitudDTO.setEstado(EstadoSolicitud.PENDIENTE);

        when(serviceSolicitud.miSolicitud()).thenReturn(solicitudDTO);

        ResponseEntity<ApiResponse<SolicitudVerificacionDTO>> response = verificacionController.miSolicitud();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(100L, response.getBody().getData().getId());
        assertEquals("Mi Empresa SAS", response.getBody().getData().getRazonSocial());
    }

    @Test
    @DisplayName("GET /api/verificacion/mi-solicitud responde 200 con data null si no hay solicitud")
    void miSolicitudRespondeOkCuandoNoHaySolicitud() {
        when(serviceSolicitud.miSolicitud()).thenReturn(null);

        ResponseEntity<ApiResponse<SolicitudVerificacionDTO>> response = verificacionController.miSolicitud();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getData());
    }

    // =========================================================
    // 5. SISTEMA DE INVITACIONES Y REGLAS DE ROLES
    // =========================================================

    @Test
    @DisplayName("Administrador solo puede invitar MODERADOR o MARKETING; falla si intenta invitar OPERADOR o ADMINISTRADOR")
    void adminRestriccionesRoles() {
        Usuario admin = new Usuario();
        admin.setId(1L);
        Rol rolAdmin = new Rol();
        rolAdmin.setNombre("ADMINISTRADOR");
        admin.setRol(rolAdmin);

        when(authHelper.usuarioAutenticado()).thenReturn(admin);

        BusinessException exOp = assertThrows(BusinessException.class,
                () -> serviceInvitacionRol.invitar("alguien@eventhive.com", "OPERADOR"));
        assertTrue(exOp.getMessage().contains("MODERADOR o MARKETING"));

        BusinessException exAdmin = assertThrows(BusinessException.class,
                () -> serviceInvitacionRol.invitar("alguien@eventhive.com", "ADMINISTRADOR"));
        assertTrue(exAdmin.getMessage().contains("MODERADOR o MARKETING"));
    }

    @Test
    @DisplayName("Representante solo puede invitar OPERADOR; falla si intenta invitar roles globales")
    void representanteRestriccionesRoles() {
        Usuario representante = new Usuario();
        representante.setId(2L);
        Rol rolRep = new Rol();
        rolRep.setNombre("REPRESENTANTE");
        representante.setRol(rolRep);

        when(authHelper.usuarioAutenticado()).thenReturn(representante);

        BusinessException exMod = assertThrows(BusinessException.class,
                () -> serviceInvitacionRol.invitar("alguien@eventhive.com", "MODERADOR"));
        assertTrue(exMod.getMessage().contains("solo pueden invitar OPERADOR"));

        BusinessException exMkt = assertThrows(BusinessException.class,
                () -> serviceInvitacionRol.invitar("alguien@eventhive.com", "MARKETING"));
        assertTrue(exMkt.getMessage().contains("solo pueden invitar OPERADOR"));
    }

    @Test
    @DisplayName("Si el envio de correo falla con CorreoException, la invitacion no queda como enviada")
    void falloEnvioCorreoPropagaExcepcionParaRollback() {
        Usuario admin = new Usuario();
        admin.setId(1L);
        Rol rolAdmin = new Rol();
        rolAdmin.setNombre("ADMINISTRADOR");
        admin.setRol(rolAdmin);

        Rol rolMod = new Rol();
        rolMod.setNombre("MODERADOR");

        when(authHelper.usuarioAutenticado()).thenReturn(admin);
        when(usuarioRepository.findByCorreoIgnoreCase("mod@eventhive.com")).thenReturn(Optional.empty());
        when(rolesRepository.findByNombre("MODERADOR")).thenReturn(Optional.of(rolMod));
        when(invitacionRolRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        org.mockito.Mockito.doThrow(new CorreoException("Error Gmail API: 401 Authentication Failed"))
                .when(serviceCorreo).enviarInvitacionRol(any(), any(), any());

        assertThrows(CorreoException.class,
                () -> serviceInvitacionRol.invitar("mod@eventhive.com", "MODERADOR"));
    }

    @Test
    @DisplayName("Admin puede invitar con éxito a rol MARKETING")
    void adminInvitaMarketingExitosamente() {
        Usuario admin = new Usuario();
        admin.setId(1L);
        Rol rolAdmin = new Rol();
        rolAdmin.setNombre("ADMINISTRADOR");
        admin.setRol(rolAdmin);

        Rol rolMkt = new Rol();
        rolMkt.setNombre("MARKETING");

        when(authHelper.usuarioAutenticado()).thenReturn(admin);
        when(usuarioRepository.findByCorreoIgnoreCase("mkt@eventhive.com")).thenReturn(Optional.empty());
        when(rolesRepository.findByNombre("MARKETING")).thenReturn(Optional.of(rolMkt));
        when(invitacionRolRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        serviceInvitacionRol.invitar("mkt@eventhive.com", "MARKETING");

        verify(serviceCorreo).enviarInvitacionRol(eq("mkt@eventhive.com"), eq("MARKETING"), any());
    }

    @Test
    @DisplayName("Actualizar foto de perfil falla limpiamente si el archivo está vacío")
    void actualizarFotoPerfilFallaSiArchivoVacio() {
        com.eventhive.app.service.ServiceUsuario serviceUsuario = mock(com.eventhive.app.service.ServiceUsuario.class);
        com.eventhive.app.service.ServiceSeguidor serviceSeguidor = mock(com.eventhive.app.service.ServiceSeguidor.class);
        UsuariosApiController controller = new UsuariosApiController(serviceUsuario, serviceSeguidor);

        org.springframework.mock.web.MockMultipartFile archivoVacio =
                new org.springframework.mock.web.MockMultipartFile("imagen", "", "image/png", new byte[0]);

        assertThrows(BusinessException.class,
                () -> controller.actualizarImagenPerfil(archivoVacio, null, null, null));
    }

    @Test
    @DisplayName("Actualizar foto de perfil acepta parámetros foto, imagen, file o avatar")
    void actualizarFotoPerfilAceptaMultiplesNombresDeParametro() {
        com.eventhive.app.service.ServiceUsuario serviceUsuario = mock(com.eventhive.app.service.ServiceUsuario.class);
        com.eventhive.app.service.ServiceSeguidor serviceSeguidor = mock(com.eventhive.app.service.ServiceSeguidor.class);
        UsuariosApiController controller = new UsuariosApiController(serviceUsuario, serviceSeguidor);

        org.springframework.mock.web.MockMultipartFile archivo =
                new org.springframework.mock.web.MockMultipartFile("foto", "perfil.jpg", "image/jpeg", new byte[]{1, 2, 3});

        when(serviceUsuario.actualizarPerfil(isNull(), any())).thenReturn(new com.eventhive.app.dto.response.UsuarioDTO());

        ResponseEntity<ApiResponse<com.eventhive.app.dto.response.UsuarioDTO>> response =
                controller.actualizarImagenPerfil(null, archivo, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(serviceUsuario).actualizarPerfil(isNull(), eq(archivo));
    }
}
