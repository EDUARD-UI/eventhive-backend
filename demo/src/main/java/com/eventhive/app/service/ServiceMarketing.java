package com.eventhive.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.dto.request.PlanPromocionRequest;
import com.eventhive.app.dto.response.EventoPromocionadoMarketingDTO;
import com.eventhive.app.dto.response.PlanPromocionDTO;
import com.eventhive.app.enums.EstadoPosicionamiento;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.PlanPromocion;
import com.eventhive.app.repository.PlanPromocionRepository;
import com.eventhive.app.repository.PosicionamientoEventoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceMarketing {

    private final PlanPromocionRepository planesRepository;
    private final PosicionamientoEventoRepository posicionamientosRepository;

    @Transactional(readOnly = true)
    public List<PlanPromocionDTO> listarPlanes() {
        return planesRepository.findAllByOrderByPrecioAsc().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<PlanPromocionDTO> listarPlanesDisponibles() {
        return planesRepository.findByActivoTrueOrderByPrecioAsc().stream().map(this::toDTO).toList();
    }

    @Transactional
    public PlanPromocionDTO actualizarPlan(Long id, PlanPromocionRequest request) {
        PlanPromocion plan = planesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de promoción no encontrado"));
        plan.setNombre(request.getNombre().trim());
        plan.setPrecio(request.getPrecio());
        plan.setComisionPorcentaje(request.getComisionPorcentaje());
        plan.setPremium(request.isPremium());
        plan.setPautaRedes(request.isPautaRedes());
        plan.setDetallePublicidad(request.getDetallePublicidad());
        plan.setActivo(request.isActivo());
        return toDTO(planesRepository.save(plan));
    }

    @Transactional(readOnly = true)
    public List<EventoPromocionadoMarketingDTO> listarEventosPromocionados() {
        return posicionamientosRepository.findByEstadoOrderByFechaContratacionDesc(
                        EstadoPosicionamiento.CONFIRMADO).stream()
                .filter(p -> p.getPlan() != null)
                .map(p -> new EventoPromocionadoMarketingDTO(
                        p.getEvento().getId(),
                        p.getEvento().getTitulo(),
                        p.getEvento().getOrganizacion().getId(),
                        p.getEvento().getOrganizacion().getRazonSocial(),
                        p.getEvento().getOrganizacion().getRepresentante().getCorreo(),
                        p.getPlan().getNombre(),
                        p.getPrecio(),
                        p.getComisionPorcentaje(),
                        Boolean.TRUE.equals(p.getPlanPremium()),
                        Boolean.TRUE.equals(p.getPautaRedes()),
                        p.getFechaContratacion()))
                .toList();
    }

    private PlanPromocionDTO toDTO(PlanPromocion plan) {
        return new PlanPromocionDTO(plan.getId(), plan.getCodigo(), plan.getNombre(),
                plan.getPrecio(), plan.getComisionPorcentaje(), plan.isPremium(),
                plan.isPautaRedes(), plan.getDetallePublicidad(), plan.isActivo());
    }
}
