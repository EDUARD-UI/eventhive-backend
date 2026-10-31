package com.eventhive.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.eventhive.app.dto.request.LocalidadRequest;
import com.eventhive.app.dto.request.PlanPromocionRequest;
import com.eventhive.app.dto.response.MarketingEstadisticasDTO;
import com.eventhive.app.dto.response.ModeracionEstadisticasDTO;
import com.eventhive.app.dto.response.PlanPromocionDTO;
import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.enums.EstadoOrganizacion;
import com.eventhive.app.enums.EstadoPosicionamiento;
import com.eventhive.app.enums.MotivosRechazos;
import com.eventhive.app.enums.PermisoEvento;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Localidad;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.PlanPromocion;
import com.eventhive.app.model.PosicionamientoEvento;
import com.eventhive.app.model.Rol;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.repository.LocalidadRepository;
import com.eventhive.app.repository.ModeracionEventoRepository;
import com.eventhive.app.repository.PlanPromocionRepository;
import com.eventhive.app.repository.PosicionamientoEventoRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

@ExtendWith(MockitoExtension.class)
class ModeracionOrganizacionMarketingTest {

    @Mock private EventoRepository eventoRepository;
    @Mock private ModeracionEventoRepository moderacionRepository;
    @Mock private LocalidadRepository localidadRepository;
    @Mock private PlanPromocionRepository planesRepository;
    @Mock private PosicionamientoEventoRepository posicionamientosRepository;
    @Mock private AuthenticatedUserHelper authHelper;
    @Mock private ServiceNotification serviceNotification;
    @Mock private ServiceEvento serviceEvento;
    @Mock private SupabaseStorageService storageService;
    @Mock private com.eventhive.app.utils.CorreoSender correoSender;
    @Mock private com.eventhive.app.utils.CorreoPlantillas correoPlantillas;
    @Mock private com.eventhive.app.repository.TiqueteRepository tiqueteRepository;

    @InjectMocks private ServiceModeracion serviceModeracion;
    @InjectMocks private ServiceLocalidad serviceLocalidad;
    @InjectMocks private ServiceMarketing serviceMarketing;
    @InjectMocks private ServiceCorreo serviceCorreo;

    // =========================================================================
    // 1. MODERACIÓN DE EVENTOS
    // =========================================================================

    @Test
    @DisplayName("Transición inválida de estado en evento lanza excepción")
    void transicionInvalidaLanzaExcepcion() {
        ServiceEvento se = new ServiceEvento(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        Evento evento = new Evento();
        evento.setEstado(EstadoEvento.BORRADOR);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                se.transicionarEstado(evento, EstadoEvento.PUBLICADO));
        assertTrue(ex.getMessage().contains("Transición no permitida"));
    }

    @Test
    @DisplayName("Estadísticas de moderación devuelve las cuatro métricas de la plataforma")
    void estadisticasModeracionCalculaLosCuatroEstados() {
        when(eventoRepository.countByEstado(EstadoEvento.PUBLICADO)).thenReturn(10L);
        when(eventoRepository.countByEstado(EstadoEvento.PENDIENTE_REVISION)).thenReturn(5L);
        when(eventoRepository.countByEstado(EstadoEvento.EN_CORRECCION)).thenReturn(3L);
        when(eventoRepository.countByEstado(EstadoEvento.RECHAZADO)).thenReturn(2L);

        ModeracionEstadisticasDTO stats = serviceModeracion.estadisticasDelModerador();

        assertNotNull(stats);
        assertEquals(10L, stats.getPublicados());
        assertEquals(5L, stats.getPendientesRevision());
        assertEquals(3L, stats.getEnCorreccion());
        assertEquals(2L, stats.getRechazados());
        // Compatibilidad con contratos existentes
        assertEquals(10L, stats.getAprobados());
        assertEquals(5L, stats.getRevisados());
        assertEquals(3L, stats.getCorreccionesSolicitadas());
    }

    @Test
    @DisplayName("Solicitar corrección valida motivo no nulo y observación requerida para OTRO")
    void solicitarCorreccionValidaMotivo() {
        assertThrows(BusinessException.class, () ->
                serviceModeracion.solicitarCorreccion(1L, null, "nota"));

        assertThrows(BusinessException.class, () ->
                serviceModeracion.solicitarCorreccion(1L, MotivosRechazos.OTRO, "   "));
    }

    // =========================================================================
    // 2. REGLAS DE ORGANIZACIÓN Y EVENTOS FINALIZADOS
    // =========================================================================

    @Test
    @DisplayName("Modificar localidades de un evento finalizado lanza BusinessException")
    void modificarLocalidadEventoFinalizadoBloqueado() {
        Evento evento = new Evento();
        evento.setId(10L);
        evento.setEstado(EstadoEvento.FINALIZADO);

        when(serviceEvento.obtenerReferenciasEvento(10L)).thenReturn(evento);

        LocalidadRequest req = new LocalidadRequest();
        req.setNombre("VIP");
        req.setPrecio(new BigDecimal("50000.00"));
        req.setCapacidad(100);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                serviceLocalidad.agregar(10L, req));
        assertTrue(ex.getMessage().contains("Los eventos finalizados no pueden modificarse"));

        BusinessException exAct = assertThrows(BusinessException.class, () ->
                serviceLocalidad.actualizar(10L, 1L, req));
        assertTrue(exAct.getMessage().contains("Los eventos finalizados no pueden modificarse"));

        BusinessException exElim = assertThrows(BusinessException.class, () ->
                serviceLocalidad.eliminar(10L, 1L));
        assertTrue(exElim.getMessage().contains("Los eventos finalizados no pueden modificarse"));
    }

    @Test
    @DisplayName("Usuario con rol distinto de ADMINISTRADOR no puede enviar correo administrativo directo")
    void enviarCorreoDirectoNoAdminBloqueado() {
        Usuario operador = new Usuario();
        operador.setNombreCompleto("Operador Org");
        operador.setCorreo("operador@org.com");
        Rol rolOperador = new Rol();
        rolOperador.setNombre("OPERADOR");
        operador.setRol(rolOperador);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                serviceCorreo.enviarCorreoAdministrativo("dest@test.com", "Asunto", "Mensaje", operador));
        assertTrue(ex.getMessage().contains("Solo los administradores pueden enviar correos directos"));
    }

    // =========================================================================
    // 3. MARKETING Y PLANES DE PROMOCIÓN
    // =========================================================================

    @Test
    @DisplayName("Crear y actualizar plan valida límite de comisión entre 0 y 100")
    void validarLimitesComisionPlan() {
        PlanPromocionRequest reqNegativo = PlanPromocionRequest.builder()
                .codigo("PLAN_X")
                .nombre("Plan X")
                .precio(new BigDecimal("1000.00"))
                .comisionPorcentaje(new BigDecimal("-5.00"))
                .build();

        BusinessException exNeg = assertThrows(BusinessException.class, () ->
                serviceMarketing.crearPlan(reqNegativo));
        assertTrue(exNeg.getMessage().contains("entre 0 y 100"));

        PlanPromocionRequest reqExcesivo = PlanPromocionRequest.builder()
                .codigo("PLAN_Y")
                .nombre("Plan Y")
                .precio(new BigDecimal("1000.00"))
                .comisionPorcentaje(new BigDecimal("105.00"))
                .build();

        BusinessException exExc = assertThrows(BusinessException.class, () ->
                serviceMarketing.crearPlan(reqExcesivo));
        assertTrue(exExc.getMessage().contains("entre 0 y 100"));
    }

    @Test
    @DisplayName("Listado de eventos promocionados desduplica por evento conservando el más reciente")
    void listarEventosPromocionadosSinDuplicados() {
        Evento evento = new Evento();
        evento.setId(100L);
        evento.setTitulo("Concierto Rock");
        Organizacion org = new Organizacion();
        org.setId(5L);
        org.setRazonSocial("Org Rock");
        Usuario rep = new Usuario();
        rep.setCorreo("rep@org.com");
        org.setRepresentante(rep);
        evento.setOrganizacion(org);

        PlanPromocion plan = new PlanPromocion();
        plan.setNombre("Plan Destacado");

        PosicionamientoEvento p1 = new PosicionamientoEvento();
        p1.setId(1L);
        p1.setEvento(evento);
        p1.setPlan(plan);
        p1.setPrecio(new BigDecimal("50000.00"));
        p1.setComisionPorcentaje(new BigDecimal("10.00"));
        p1.setFechaContratacion(java.time.LocalDateTime.now());

        PosicionamientoEvento p2 = new PosicionamientoEvento();
        p2.setId(2L);
        p2.setEvento(evento);
        p2.setPlan(plan);
        p2.setPrecio(new BigDecimal("40000.00"));
        p2.setComisionPorcentaje(new BigDecimal("10.00"));
        p2.setFechaContratacion(java.time.LocalDateTime.now().minusDays(10));

        when(posicionamientosRepository.findByEstadoOrderByFechaContratacionDesc(EstadoPosicionamiento.CONFIRMADO))
                .thenReturn(List.of(p1, p2));

        var resultado = serviceMarketing.listarEventosPromocionados();
        assertEquals(1, resultado.size());
        assertEquals(100L, resultado.get(0).getEventoId());
        assertEquals(new BigDecimal("50000.00"), resultado.get(0).getPrecioPagado());
    }

    @Test
    @DisplayName("Estadísticas de marketing calcula eventos activos y eventos por plan")
    void estadisticasMarketingCalculoCorrecto() {
        when(posicionamientosRepository.contarEventosActivosConPromocionPagada()).thenReturn(8L);

        Object[] filaPlan1 = new Object[]{1L, 5L};
        Object[] filaPlan2 = new Object[]{2L, 3L};
        when(posicionamientosRepository.contarEventosPorPlan()).thenReturn(List.of(filaPlan1, filaPlan2));

        PlanPromocion pl1 = new PlanPromocion();
        pl1.setId(1L);
        pl1.setCodigo("PLAN_1");
        pl1.setNombre("Plan Básico");

        PlanPromocion pl2 = new PlanPromocion();
        pl2.setId(2L);
        pl2.setCodigo("PLAN_2");
        pl2.setNombre("Plan Premium");

        when(planesRepository.findAllByOrderByPrecioAsc()).thenReturn(List.of(pl1, pl2));

        MarketingEstadisticasDTO stats = serviceMarketing.obtenerEstadisticas();
        assertNotNull(stats);
        assertEquals(8L, stats.getEventosActivosPromocionados());
        assertEquals(2, stats.getEventosPorPlan().size());
        assertEquals(5L, stats.getEventosPorPlan().get(0).getCantidadEventos());
        assertEquals(3L, stats.getEventosPorPlan().get(1).getCantidadEventos());
    }
}
