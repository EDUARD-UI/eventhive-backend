package com.eventhive.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventhive.app.dto.request.EventoRequest;
import com.eventhive.app.auto.ResultadoReglaModeracion;
import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.enums.EstadoOrganizacion;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.model.Categoria;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.ModeracionEvento;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.CategoriaRepository;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.repository.LocalidadRepository;
import com.eventhive.app.repository.ModeracionEventoRepository;
import com.eventhive.app.repository.OrganizacionRepository;
import com.eventhive.app.repository.TiqueteRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import com.eventhive.app.config.SupabaseStorageConfig;
import com.eventhive.app.auto.ServiceModeracionAutomatica;

@ExtendWith(MockitoExtension.class)
class ServiceEventoCreacionTest {

    @Mock
    private ServiceOrganizacion serviceOrganizacion;
    @Mock
    private ServiceNivelOrganizacion serviceNivelOrganizacion;
    @Mock
    private ServiceNotification serviceNotification;
    @Mock
    private ServiceModeracionAutomatica serviceModeracionAutomatica;
    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private LocalidadRepository localidadRepository;
    @Mock
    private TiqueteRepository tiqueteRepository;
    @Mock
    private ModeracionEventoRepository moderacionEventoRepository;
    @Mock
    private OrganizacionRepository organizacionRepository;
    @Mock
    private AuthenticatedUserHelper authHelper;
    @Mock
    private SupabaseStorageService storageService;
    @Mock
    private SupabaseStorageConfig storageConfig;
    @InjectMocks
    private ServiceEvento serviceEvento;

    @Test
    void organizacionPendientePuedeGuardarEventoComoBorrador() {
        Usuario representante = representante(7L, EstadoOrganizacion.PENDIENTE_REVISION);
        Categoria categoria = new Categoria();
        when(authHelper.usuarioAutenticado()).thenReturn(representante);
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(categoria));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> {
            Evento evento = invocation.getArgument(0);
            evento.setId(12L);
            return evento;
        });
        when(localidadRepository.countByEventoId(12L)).thenReturn(1L);

        Evento resultado = serviceEvento.crearEvento(request(), null);

        assertEquals(EstadoEvento.BORRADOR, resultado.getEstado());
        assertEquals(EstadoOrganizacion.PENDIENTE_REVISION, resultado.getOrganizacion().getEstado());
    }

    @Test
    void organizacionSuspendidaNoPuedeCrearEvento() {
        Usuario representante = representante(7L, EstadoOrganizacion.SUSPENDIDA);
        when(authHelper.usuarioAutenticado()).thenReturn(representante);
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(new Categoria()));

        assertThrows(BusinessException.class, () -> serviceEvento.crearEvento(request(), null));
        verifyNoInteractions(eventoRepository);
    }

    @Test
    void organizacionAprobadaEnviaBorradorAlFlujoDeModeracionExistente() {
        Usuario representante = representante(7L, EstadoOrganizacion.APROBADA);
        Evento evento = new Evento();
        evento.setId(12L);
        evento.setEstado(EstadoEvento.BORRADOR);
        evento.setOrganizacion(representante.getOrganizacion());
        when(authHelper.usuarioAutenticado()).thenReturn(representante);
        when(eventoRepository.findByIdConReferencias(12L)).thenReturn(Optional.of(evento));
        when(eventoRepository.countActivosByOrganizacionId(3L)).thenReturn(0L);
        when(serviceModeracionAutomatica.evaluarEvento(evento)).thenReturn(ResultadoReglaModeracion.ok());

        serviceEvento.enviarRevision(12L);

        assertEquals(EstadoEvento.PENDIENTE_REVISION, evento.getEstado());
        org.mockito.Mockito.verify(serviceModeracionAutomatica).evaluarEvento(evento);
        org.mockito.Mockito.verify(moderacionEventoRepository).save(any(ModeracionEvento.class));
    }

    private Usuario representante(Long id, EstadoOrganizacion estado) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        Organizacion organizacion = new Organizacion();
        organizacion.setId(3L);
        organizacion.setEstado(estado);
        organizacion.setRepresentante(usuario);
        usuario.setOrganizacion(organizacion);
        return usuario;
    }

    private EventoRequest request() {
        EventoRequest request = new EventoRequest();
        request.setTitulo("Evento de prueba");
        request.setDescripcion("Descripción");
        request.setLugar("Lugar");
        request.setFecha(LocalDate.now().plusDays(2));
        request.setHora(LocalTime.NOON);
        request.setCategoriaId(2L);
        request.setLatitud(4.6);
        request.setLongitud(-74.1);
        return request;
    }
}