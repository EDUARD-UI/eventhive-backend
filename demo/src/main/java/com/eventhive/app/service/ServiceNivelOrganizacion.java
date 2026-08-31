package com.eventhive.app.service;

import com.eventhive.app.dto.response.SugerenciaAscensoDTO;
import com.eventhive.app.enums.EstadoSolicitud;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.SugerenciaAscenso;
import com.eventhive.app.repository.OrganizacionRepository;
import com.eventhive.app.repository.SugerenciaAscensoRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


@Service
@RequiredArgsConstructor
public class ServiceNivelOrganizacion {

    private final SugerenciaAscensoRepository sugerenciaRepository;
    private final OrganizacionRepository organizacionRepository;
    private final AuthenticatedUserHelper authHelper;

    // Requisitos mínimos por nivel para avanzar al siguiente: eventos finalizados y antigüedad en días
    private static final int[] FINALIZADOS_REQUERIDOS = {3, 8};
    private static final int[] ANTIGUEDAD_DIAS_REQUERIDA = {30, 90};

    //CONSULTAS
    @Transactional(readOnly = true)
    public Page<SugerenciaAscensoDTO> listarPendientes(Pageable pageable) {
        return sugerenciaRepository.findByEstadoConOrganizacion(EstadoSolicitud.PENDIENTE, pageable).map(this::toDTO);
    }

    // Se llama cada vez que un evento del organizador se finaliza o se rechaza
    @Transactional
    public void evaluarAscenso(Organizacion organizacion) {
        if (organizacion.getNivel() == null || organizacion.getNivel().esMaximo()) return;
        if (organizacion.getEventosRechazados() > 0) return;
        if (sugerenciaRepository.existsByOrganizacionIdAndEstado(organizacion.getId(), EstadoSolicitud.PENDIENTE))
            return;

        int indice = organizacion.getNivel().ordinal();
        long antiguedadDias = ChronoUnit.DAYS.between(organizacion.getFechaCreacion(), LocalDateTime.now());

        boolean cumple = organizacion.getEventosFinalizados() >= FINALIZADOS_REQUERIDOS[indice]
                && antiguedadDias >= ANTIGUEDAD_DIAS_REQUERIDA[indice];

        if (!cumple) return;

        SugerenciaAscenso sugerencia = new SugerenciaAscenso();
        sugerencia.setOrganizacion(organizacion);
        sugerencia.setNivelActual(organizacion.getNivel());
        sugerencia.setNivelSugerido(organizacion.getNivel().siguiente());
        sugerenciaRepository.save(sugerencia);
    }

    //OPERACIONES DE ASCENSO
    @Transactional
    public void aprobarAscenso(Long id) {
        SugerenciaAscenso sugerencia = obtenerPendiente(id);

        Organizacion organizacion = sugerencia.getOrganizacion();
        organizacion.setNivel(sugerencia.getNivelSugerido());
        organizacionRepository.save(organizacion);

        resolver(sugerencia, EstadoSolicitud.APROBADA);
    }

    @Transactional
    public void rechazarAscenso(Long id) {
        resolver(obtenerPendiente(id), EstadoSolicitud.RECHAZADA);
    }

    //METODOS AUXILIARES Y DE MAPEO
    private SugerenciaAscenso obtenerPendiente(Long id) {
        SugerenciaAscenso s = sugerenciaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Sugerencia no encontrada"));
        if (s.getEstado() != EstadoSolicitud.PENDIENTE)
            throw new BusinessException("Solo se pueden gestionar sugerencias en estado PENDIENTE");
        return s;
    }

    private void resolver(SugerenciaAscenso sugerencia, EstadoSolicitud resultado) {
        sugerencia.setEstado(resultado);
        sugerencia.setFechaResolucion(LocalDateTime.now());
        sugerencia.setAdministradorQueResolvi(authHelper.usuarioAutenticado());
        sugerenciaRepository.save(sugerencia);
    }

    private SugerenciaAscensoDTO toDTO(SugerenciaAscenso s) {
        SugerenciaAscensoDTO dto = new SugerenciaAscensoDTO();
        dto.setId(s.getId());
        dto.setNivelActual(s.getNivelActual());
        dto.setNivelSugerido(s.getNivelSugerido());
        dto.setEstado(s.getEstado());
        dto.setFechaGeneracion(s.getFechaGeneracion());
        dto.setFechaResolucion(s.getFechaResolucion());
        if (s.getOrganizacion() != null) {
            dto.setOrganizacionId(s.getOrganizacion().getId());
            dto.setOrganizacionNombre(s.getOrganizacion().getRazonSocial());
        }
        return dto;
    }
}
