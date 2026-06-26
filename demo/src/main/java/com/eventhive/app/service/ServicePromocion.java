package com.eventhive.app.service;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.dto.PromocionDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Promocion;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.repository.PromocionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServicePromocion {

    private final PromocionRepository promocionRepository;
    private final EventoRepository    eventoRepository;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Transactional(readOnly = true)
    public List<Promocion> obtenerPorEvento(Long eventoId) {
        return promocionRepository.findByEventoId(eventoId);
    }

    @Transactional(readOnly = true)
    public Page<PromocionDTO> obtenerTodasPromociones(Pageable pageable) {
        return promocionRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<PromocionDTO> obtenerDTOPorOrganizador(Long organizadorId, Pageable pageable) {
        return promocionRepository.findByOrganizadorId(organizadorId, pageable).map(this::toDTO);
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public void crearPromocion(Long eventoId, String descripcion, BigDecimal descuento,
                               String fechaInicio, String fechaFin, Usuario organizador) {
        validarDescuento(descuento);
        LocalDate inicio = LocalDate.parse(fechaInicio, FMT);
        LocalDate fin    = LocalDate.parse(fechaFin, FMT);
        validarFechas(inicio, fin);

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado: " + eventoId));

        if (!esAdmin(organizador) && !evento.getOrganizador().getId().equals(organizador.getId()))
            throw new BusinessException("No autorizado: el evento no te pertenece");

        // Un evento no puede tener dos promociones activas en el mismo rango de fechas
        if (promocionRepository.existsConflictoFechas(eventoId, inicio, fin, null))
            throw new BusinessException(
                    "El evento ya tiene una promoción activa en ese rango de fechas");

        Promocion p = new Promocion();
        p.setDescripcion(descripcion);
        p.setDescuento(descuento.doubleValue());
        p.setFechaInicio(inicio);
        p.setFechaFin(fin);
        p.setEventos(new ArrayList<>(List.of(evento)));
        promocionRepository.save(p);
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public void actualizarPromocion(Long id, Long eventoId, String descripcion,
                                    BigDecimal descuento, String fechaInicio,
                                    String fechaFin, Usuario organizador) {
        Promocion p = obtenerPromocionPorId(id);
        validarPermiso(p, organizador);
        validarDescuento(descuento);
        LocalDate inicio = LocalDate.parse(fechaInicio, FMT);
        LocalDate fin    = LocalDate.parse(fechaFin, FMT);
        validarFechas(inicio, fin);

        if (eventoId != null) {
            Evento nuevoEvento = eventoRepository.findById(eventoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado: " + eventoId));

            if (!esAdmin(organizador) && !nuevoEvento.getOrganizador().getId().equals(organizador.getId()))
                throw new BusinessException("No autorizado: el evento no te pertenece");

            // Excluir la propia promoción al verificar conflicto (permite guardar sin cambios de fecha)
            if (promocionRepository.existsConflictoFechas(eventoId, inicio, fin, id))
                throw new BusinessException(
                        "El evento ya tiene una promoción activa en ese rango de fechas");

            p.setEventos(new ArrayList<>(List.of(nuevoEvento)));
        }

        p.setDescripcion(descripcion);
        p.setDescuento(descuento.doubleValue());
        p.setFechaInicio(inicio);
        p.setFechaFin(fin);
        promocionRepository.save(p);
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public void eliminarPromocion(Long id, Usuario organizador) {
        Promocion p = obtenerPromocionPorId(id);
        validarPermiso(p, organizador);
        promocionRepository.deleteById(id);
    }

    // --- helpers privados ---

    private Promocion obtenerPromocionPorId(Long id) {
        return promocionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada: " + id));
    }

    private void validarDescuento(BigDecimal d) {
        if (d.compareTo(BigDecimal.ONE) < 0 || d.compareTo(new BigDecimal("75")) > 0)
            throw new BusinessException("El descuento debe estar entre 1 y 75");
    }

    private void validarFechas(LocalDate inicio, LocalDate fin) {
        if (fin.isBefore(inicio))
            throw new BusinessException("La fecha de fin no puede ser anterior a la de inicio");
    }

    private boolean esAdmin(Usuario u) {
        return u.getRol() != null && "ADMINISTRADOR".equals(u.getRol().getNombre());
    }

    private void validarPermiso(Promocion p, Usuario organizador) {
        if (esAdmin(organizador)) return;
        boolean autorizado = p.getEventos() != null && p.getEventos().stream()
                .anyMatch(e -> e.getOrganizador() != null
                        && e.getOrganizador().getId().equals(organizador.getId()));
        if (!autorizado)
            throw new BusinessException("No autorizado para modificar esta promoción");
    }

    private PromocionDTO toDTO(Promocion p) {
        PromocionDTO dto = new PromocionDTO();
        dto.setId(p.getId());
        dto.setDescripcion(p.getDescripcion());
        dto.setDescuento(BigDecimal.valueOf(p.getDescuento()));
        dto.setFechaInicio(p.getFechaInicio());
        dto.setFechaFinal(p.getFechaFin());
        if (p.getEventos() != null) {
            p.getEventos().stream().filter(e -> e != null).findFirst().ifPresent(e -> {
                dto.setEventoId(e.getId());
                dto.setEventoTitulo(e.getTitulo());
            });
        }
        return dto;
    }
}