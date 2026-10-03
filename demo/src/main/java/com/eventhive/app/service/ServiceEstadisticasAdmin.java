package com.eventhive.app.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.dto.response.EventoEstadoConteoDTO;
import com.eventhive.app.dto.response.EventosPorCategoriaDTO;
import com.eventhive.app.dto.response.IngresosPlataformaDTO;
import com.eventhive.app.dto.response.OrganizacionEstadoConteoDTO;
import com.eventhive.app.dto.response.OrganizacionesPorValidacionDTO;
import com.eventhive.app.dto.response.TopEventoVentasDTO;
import com.eventhive.app.dto.response.TopOrganizacionVentasDTO;
import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.enums.EstadoOrganizacion;
import com.eventhive.app.enums.EstadoSolicitud;
import com.eventhive.app.repository.CategoriaRepository;
import com.eventhive.app.repository.CompraRepository;
import com.eventhive.app.repository.PosicionamientoEventoRepository;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.repository.ItemCompraRepository;
import com.eventhive.app.repository.OrganizacionRepository;
import com.eventhive.app.repository.SolicitudVerificacionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceEstadisticasAdmin {

    private static final int TOP = 5;

    private final OrganizacionRepository organizacionRepository;
    private final SolicitudVerificacionRepository solicitudRepository;
    private final EventoRepository eventoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ItemCompraRepository itemCompraRepository;
    private final CompraRepository compraRepository;
    private final PosicionamientoEventoRepository posicionamientoEventoRepository;
    
    //CONSULTAS
    public OrganizacionesPorValidacionDTO organizacionesPorValidacion() {
        Map<EstadoOrganizacion, Long> conteo = organizacionRepository.contarPorEstado().stream()
                .collect(Collectors.toMap(OrganizacionEstadoConteoDTO::getEstado, OrganizacionEstadoConteoDTO::getCantidad));

        List<OrganizacionEstadoConteoDTO> porEstado = Arrays.stream(EstadoOrganizacion.values())
                .map(e -> new OrganizacionEstadoConteoDTO(e, conteo.getOrDefault(e, 0L)))
                .toList();
        long total = porEstado.stream().mapToLong(OrganizacionEstadoConteoDTO::getCantidad).sum();

        return new OrganizacionesPorValidacionDTO(
                solicitudRepository.countByEstado(EstadoSolicitud.PENDIENTE),
                solicitudRepository.countByEstado(EstadoSolicitud.INCOMPLETA),
                total,
                porEstado);
    }

    public List<EventoEstadoConteoDTO> eventosPorEstado() {
        Map<EstadoEvento, Long> conteo = eventoRepository.contarPorEstado().stream()
                .collect(Collectors.toMap(EventoEstadoConteoDTO::getEstado, EventoEstadoConteoDTO::getCantidad));

        return Arrays.stream(EstadoEvento.values())
                .map(e -> new EventoEstadoConteoDTO(e, conteo.getOrDefault(e, 0L)))
                .toList();
    }

    public List<EventosPorCategoriaDTO> eventosPorCategoria() {
        return categoriaRepository.contarEventosPorCategoria();
    }

    public List<TopEventoVentasDTO> topEventosPorVentas() {
        return itemCompraRepository.topEventosPorVentas(PageRequest.of(0, TOP));
    }

    public List<TopOrganizacionVentasDTO> topOrganizacionesPorVentas() {
        return itemCompraRepository.topOrganizacionesPorVentas(PageRequest.of(0, TOP));
    }

    public IngresosPlataformaDTO ingresosPlataforma() {
        java.math.BigDecimal comisiones = compraRepository.sumarComisionesConfirmadas();
        java.math.BigDecimal comisionesPosicionados = compraRepository
                .sumarComisionesPosicionados(ServiceMonetizacion.COMISION_NORMAL);
        java.math.BigDecimal posicionamientos = posicionamientoEventoRepository.sumarIngresosConfirmados();

        return new IngresosPlataformaDTO(
                comisiones, comisionesPosicionados, posicionamientos, comisiones.add(posicionamientos));
    }
}
