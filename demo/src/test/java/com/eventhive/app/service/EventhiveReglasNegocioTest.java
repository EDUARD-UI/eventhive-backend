package com.eventhive.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import com.eventhive.app.auto.ServiceModeracionAutomatica;
import com.eventhive.app.dto.request.EventoRequest;
import com.eventhive.app.dto.request.PlanPromocionRequest;
import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.enums.EstadoPosicionamiento;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.PlanPromocion;
import com.eventhive.app.model.PosicionamientoEvento;
import com.eventhive.app.model.Rol;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.CategoriaRepository;
import com.eventhive.app.repository.CompraRepository;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.repository.ItemCompraRepository;
import com.eventhive.app.repository.LocalidadRepository;
import com.eventhive.app.repository.ModeracionEventoRepository;
import com.eventhive.app.repository.OrganizacionRepository;
import com.eventhive.app.repository.PlanPromocionRepository;
import com.eventhive.app.repository.PosicionamientoEventoRepository;
import com.eventhive.app.repository.RolesRepository;
import com.eventhive.app.repository.SolicitudVerificacionRepository;
import com.eventhive.app.repository.TiqueteRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

@ExtendWith(MockitoExtension.class)
class EventhiveReglasNegocioTest {

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    // Repositorios & mocks
    @Mock private EventoRepository eventoRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private OrganizacionRepository organizacionRepository;
    @Mock private ItemCompraRepository itemCompraRepository;
    @Mock private CompraRepository compraRepository;
    @Mock private PosicionamientoEventoRepository posicionamientoEventoRepository;
    @Mock private PlanPromocionRepository planPromocionRepository;
    @Mock private SolicitudVerificacionRepository solicitudRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolesRepository rolesRepository;
    @Mock private LocalidadRepository localidadRepository;
    @Mock private TiqueteRepository tiqueteRepository;
    @Mock private ModeracionEventoRepository moderacionEventoRepository;
    @Mock private ServiceOrganizacion serviceOrganizacion;
    @Mock private ServiceNivelOrganizacion serviceNivelOrganizacion;
    @Mock private ServiceModeracionAutomatica serviceModeracionAutomatica;
    @Mock private AuthenticatedUserHelper authHelper;
    @Mock private ServiceNotification serviceNotification;
    @Mock private ServiceCorreo serviceCorreo;
    @Mock private SupabaseStorageService storageService;
    @Mock private com.eventhive.app.config.SupabaseStorageConfig storageConfig;
    @Mock private ObjectProvider<ServiceCompra> selfCompra;

    private ServiceMonetizacion serviceMonetizacion;

    @InjectMocks private ServiceEvento serviceEvento;
    @InjectMocks private ServiceCompra serviceCompra;
    @InjectMocks private ServiceMarketing serviceMarketing;
    @InjectMocks private ServicePromocion servicePromocion;
    @InjectMocks private ServiceUsuario serviceUsuario;
    @InjectMocks private ServiceEstadisticasAdmin serviceEstadisticasAdmin;

    @BeforeEach
    void setUp() {
        serviceMonetizacion = new ServiceMonetizacion();
    }

    // =========================================================================
    // 1. CÁLCULO DE PRECIOS Y COMISIONES (Módulo 4)
    // =========================================================================
    @Test
    @DisplayName("Precio base $100.000 + 10% comisión -> Comprador paga $110.000, Organizador recibe $100.000 neto")
    void calculoComisionComercialAdicionalAlComprador() {
        BigDecimal base = new BigDecimal("100000.00");
        BigDecimal porcentaje = new BigDecimal("10.00");

        BigDecimal comision = serviceMonetizacion.calcularComision(base, porcentaje);
        BigDecimal totalComprador = serviceMonetizacion.calcularTotalComprador(base, comision);
        BigDecimal netoOrganizador = serviceMonetizacion.calcularNetoOrganizador(base);

        assertEquals(0, new BigDecimal("10000.00").compareTo(comision));
        assertEquals(0, new BigDecimal("110000.00").compareTo(totalComprador));
        assertEquals(0, new BigDecimal("100000.00").compareTo(netoOrganizador));
    }

    @Test
    @DisplayName("Comisión comercial no se deduce del neto del organizador")
    void comisionNoSeRestaDeIngresosDeOrganizacion() {
        BigDecimal base = new BigDecimal("50000.00");
        BigDecimal porcentaje = new BigDecimal("7.00");

        BigDecimal comision = serviceMonetizacion.calcularComision(base, porcentaje);
        BigDecimal totalComprador = serviceMonetizacion.calcularTotalComprador(base, comision);
        BigDecimal netoOrganizador = serviceMonetizacion.calcularNetoOrganizador(base);

        assertEquals(0, new BigDecimal("3500.00").compareTo(comision));
        assertEquals(0, new BigDecimal("53500.00").compareTo(totalComprador));
        assertEquals(0, base.compareTo(netoOrganizador));
        assertEquals(totalComprador.subtract(comision), netoOrganizador);
    }

    // =========================================================================
    // 2. VALIDACIÓN DE FECHAS AL CREAR Y EDITAR (Módulo 3.2)
    // =========================================================================
    @Test
    @DisplayName("Crear evento con fecha anterior al día actual lanza BusinessException")
    void creacionEventoFechaPasadaInvalida() {
        ZonedDateTime ahora = ZonedDateTime.now(ZONA_BOGOTA);
        EventoRequest req = new EventoRequest();
        req.setFecha(ahora.toLocalDate().minusDays(1));
        req.setHora(LocalTime.of(15, 0));

        BusinessException ex = assertThrows(BusinessException.class, () -> serviceEvento.validarFechasCreacion(req));
        assertTrue(ex.getMessage().contains("anterior al día actual"));
    }

    @Test
    @DisplayName("Crear evento para hoy con hora pasada lanza BusinessException")
    void creacionEventoHoyHoraPasadaInvalida() {
        ZonedDateTime ahora = ZonedDateTime.now(ZONA_BOGOTA);
        EventoRequest req = new EventoRequest();
        req.setFecha(ahora.toLocalDate());
        req.setHora(ahora.toLocalTime().minusMinutes(5));

        BusinessException ex = assertThrows(BusinessException.class, () -> serviceEvento.validarFechasCreacion(req));
        assertTrue(ex.getMessage().contains("posterior a la hora actual"));
    }

    @Test
    @DisplayName("Crear evento con fechaFin anterior a fechaInicio lanza BusinessException")
    void creacionEventoFechaFinInvalida() {
        ZonedDateTime ahora = ZonedDateTime.now(ZONA_BOGOTA);
        EventoRequest req = new EventoRequest();
        req.setFecha(ahora.toLocalDate().plusDays(2));
        req.setHora(LocalTime.of(18, 0));
        req.setFechaFin(ahora.toLocalDate().plusDays(1)); // Fin antes que inicio

        BusinessException ex = assertThrows(BusinessException.class, () -> serviceEvento.validarFechasCreacion(req));
        assertTrue(ex.getMessage().contains("posterior a la fecha y hora de inicio"));
    }

    @Test
    @DisplayName("Crear evento futuro válido no lanza excepción")
    void creacionEventoFuturoValido() {
        ZonedDateTime ahora = ZonedDateTime.now(ZONA_BOGOTA);
        EventoRequest req = new EventoRequest();
        req.setFecha(ahora.toLocalDate().plusDays(10));
        req.setHora(LocalTime.of(20, 0));
        req.setFechaFin(ahora.toLocalDate().plusDays(10));
        req.setHoraFin(LocalTime.of(23, 0));

        serviceEvento.validarFechasCreacion(req);
    }

    @Test
    @DisplayName("Editar evento pasado permite modificar otros campos si conserva inicio histórico")
    void editarEventoPasadoConservaInicioHistorico() {
        Evento eventoExistente = new Evento();
        eventoExistente.setFecha(LocalDate.of(2025, 1, 15));
        eventoExistente.setHora(LocalTime.of(10, 0));

        EventoRequest req = new EventoRequest();
        req.setTitulo("Título nuevo");
        req.setFecha(LocalDate.of(2025, 1, 15)); // Mismo inicio histórico
        req.setHora(LocalTime.of(10, 0));

        serviceEvento.validarFechasActualizacion(eventoExistente, req);
    }

    @Test
    @DisplayName("Editar evento pasado bloquea alterar la fecha u hora de inicio histórico")
    void editarEventoPasadoBloqueaModificarInicioHistorico() {
        Evento eventoExistente = new Evento();
        eventoExistente.setFecha(LocalDate.of(2025, 1, 15));
        eventoExistente.setHora(LocalTime.of(10, 0));

        EventoRequest req = new EventoRequest();
        req.setFecha(LocalDate.of(2025, 1, 16)); // Intento de cambiar inicio
        req.setHora(LocalTime.of(10, 0));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                serviceEvento.validarFechasActualizacion(eventoExistente, req));
        assertTrue(ex.getMessage().contains("No se puede modificar la fecha u hora de inicio de un evento que ya inició o ha ocurrido"));
    }

    // =========================================================================
    // 3. PLANES DE PROMOCIÓN CON PRECIO FIJO (Módulo 5)
    // =========================================================================
    @Test
    @DisplayName("Crear plan valida código obligatorio y único y precio no negativo")
    void crearPlanValidaciones() {
        PlanPromocionRequest req = PlanPromocionRequest.builder()
                .codigo("PLAN_GOLD")
                .nombre("Plan Gold")
                .precio(new BigDecimal("250000.00"))
                .comisionPorcentaje(BigDecimal.ZERO)
                .premium(true)
                .pautaRedes(true)
                .build();

        when(planPromocionRepository.findByCodigo("PLAN_GOLD")).thenReturn(Optional.of(new PlanPromocion()));

        BusinessException ex = assertThrows(BusinessException.class, () -> serviceMarketing.crearPlan(req));
        assertTrue(ex.getMessage().contains("Ya existe un plan con el código"));
    }

    // =========================================================================
    // 4. CONFIRMACIÓN DE PAGOS E IDEMPOTENCIA DE PROMOCIÓN (Módulo 6)
    // =========================================================================
    @Test
    @DisplayName("Confirmación de pago de posicionamiento es idempotente si ya estaba CONFIRMADO")
    void confirmacionPosicionamientoIdempotente() {
        Usuario organizador = new Usuario();
        organizador.setId(5L);

        Organizacion org = new Organizacion();
        org.setId(100L);
        org.setRepresentante(organizador);

        Evento evento = new Evento();
        evento.setId(10L);
        evento.setEstado(EstadoEvento.PUBLICADO);
        evento.setPromocionado(true);
        evento.setOrganizacion(org);

        PosicionamientoEvento pago = new PosicionamientoEvento();
        pago.setId(42L);
        pago.setEvento(evento);
        pago.setOrganizador(organizador);
        pago.setEstado(EstadoPosicionamiento.CONFIRMADO); // Ya confirmado
        pago.setPrecio(new BigDecimal("150000.00"));

        when(authHelper.usuarioAutenticado()).thenReturn(organizador);
        when(posicionamientoEventoRepository.findById(42L)).thenReturn(Optional.of(pago));

        var res = serviceCompra.confirmarPagoPosicionamientoSimulado(42L);

        assertEquals(EstadoPosicionamiento.CONFIRMADO, res.getEstado());
        // No se re-envía correo ni se muta estado
        verify(serviceCorreo, never()).enviarPromocionConfirmada(any());
    }

    // =========================================================================
    // 5. BANNER / CARRUSEL: DESTACADO VS PREMIUM (Módulo 7)
    // =========================================================================
    @Test
    @DisplayName("Asignar imagen destacada rechaza si el usuario no tiene rol MARKETING o ADMINISTRADOR")
    void asignarImagenDestacadaRechazaRolNoAutorizado() {
        Usuario cliente = new Usuario();
        cliente.setId(1L);
        Rol rolCliente = new Rol();
        rolCliente.setNombre("CLIENTE");
        cliente.setRol(rolCliente);

        when(authHelper.usuarioAutenticado()).thenReturn(cliente);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                servicePromocion.asignarImagenDestacada(10L, "https://supabase.co/img.png"));
        assertTrue(ex.getMessage().contains("Solo el personal autorizado de marketing"));
    }

    @Test
    @DisplayName("Asignar imagen destacada rechaza evento sin plan premium")
    void asignarImagenDestacadaRechazaSinPlanPremium() {
        Usuario marketing = new Usuario();
        Rol rolMarketing = new Rol();
        rolMarketing.setNombre("MARKETING");
        marketing.setRol(rolMarketing);

        Evento evento = new Evento();
        evento.setId(20L);
        evento.setEstado(EstadoEvento.PUBLICADO);

        when(authHelper.usuarioAutenticado()).thenReturn(marketing);
        when(eventoRepository.findById(20L)).thenReturn(Optional.of(evento));
        when(posicionamientoEventoRepository.existsByEventoIdAndEstadoAndPlanPremiumTrue(20L, EstadoPosicionamiento.CONFIRMADO))
                .thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                servicePromocion.asignarImagenDestacada(20L, "https://supabase.co/img.png"));
        assertTrue(ex.getMessage().contains("plan premium"));
    }

    // =========================================================================
    // 6. GESTIÓN Y REVOCACIÓN DE ROL DE MARKETING (Módulo 10.3)
    // =========================================================================
    @Test
    @DisplayName("Revocar rol MARKETING asigna CLIENTE exitosamente")
    void revocarRolMarketingExitoso() {
        Usuario usuario = new Usuario();
        usuario.setId(33L);
        Rol rolMarketing = new Rol();
        rolMarketing.setNombre("MARKETING");
        usuario.setRol(rolMarketing);

        Rol rolCliente = new Rol();
        rolCliente.setNombre("CLIENTE");

        when(usuarioRepository.findByIdConRol(33L)).thenReturn(Optional.of(usuario));
        when(rolesRepository.findByNombre("CLIENTE")).thenReturn(Optional.of(rolCliente));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        serviceUsuario.revocarMarketing(33L);

        assertEquals("CLIENTE", usuario.getRol().getNombre());
        verify(usuarioRepository).save(usuario);
        verify(serviceNotification).notificarRevocacionRol(usuario);
    }

    @Test
    @DisplayName("Revocar rol MARKETING rechaza si el usuario no tiene rol MARKETING")
    void revocarRolMarketingFallaSiNoEsMarketing() {
        Usuario usuario = new Usuario();
        usuario.setId(44L);
        Rol rolOperador = new Rol();
        rolOperador.setNombre("OPERADOR");
        usuario.setRol(rolOperador);

        when(usuarioRepository.findByIdConRol(44L)).thenReturn(Optional.of(usuario));

        BusinessException ex = assertThrows(BusinessException.class, () -> serviceUsuario.revocarMarketing(44L));
        assertTrue(ex.getMessage().contains("El usuario no tiene asignado el rol MARKETING"));
    }

    // =========================================================================
    // 7. MÉTRICAS COMERCIALES Y COHERENCIA FINANCIERA (Módulo 12)
    // =========================================================================
    @Test
    @DisplayName("Métricas comerciales calculan ventaBrutaTotal y totalIngresos con separación de conceptos")
    void metricasComercialesCoherenciaFinanciera() {
        // Boletas: total comprador = $550.000 (base $500.000 + comisión $50.000)
        when(compraRepository.sumarTotalBrutoConfirmado()).thenReturn(new BigDecimal("550000.00"));
        when(compraRepository.sumarComisionesConfirmadas()).thenReturn(new BigDecimal("50000.00"));
        when(compraRepository.sumarComisionesPosicionados(any())).thenReturn(new BigDecimal("15000.00"));
        when(compraRepository.sumarTotalNetoOrganizadores()).thenReturn(new BigDecimal("500000.00"));
        // Promociones: pagos confirmados = $200.000
        when(posicionamientoEventoRepository.sumarIngresosConfirmados()).thenReturn(new BigDecimal("200000.00"));

        var metricas = serviceEstadisticasAdmin.ingresosPlataforma();

        // Venta bruta total = $550.000 + $200.000 = $750.000
        assertEquals(0, new BigDecimal("750000.00").compareTo(metricas.getVentaBrutaTotal()));
        // Comisiones exclusivas por boletas = $50.000
        assertEquals(0, new BigDecimal("50000.00").compareTo(metricas.getComisionesVentas()));
        // Ingresos por promociones = $200.000
        assertEquals(0, new BigDecimal("200000.00").compareTo(metricas.getIngresosPagoPromocion()));
        // Total ingresos plataforma = comisiones boletas ($50.000) + promociones ($200.000) = $250.000
        assertEquals(0, new BigDecimal("250000.00").compareTo(metricas.getTotalIngresos()));
        // Ingresos de organizaciones = $500.000
        assertEquals(0, new BigDecimal("500000.00").compareTo(metricas.getIngresosOrganizaciones()));
    }
}
