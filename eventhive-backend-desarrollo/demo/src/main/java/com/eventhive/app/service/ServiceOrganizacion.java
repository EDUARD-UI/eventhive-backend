package com.eventhive.app.service;

import com.eventhive.app.dto.response.OrganizacionDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.OrganizacionRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Solo expone datos de organizaciones ya APROBADAS
@Service
@RequiredArgsConstructor
public class ServiceOrganizacion {

    private final OrganizacionRepository organizacionRepository;
    private final AuthenticatedUserHelper authHelper;

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ORGANIZACION')")
    public OrganizacionDTO miOrganizacion() {
        Usuario usuario = authHelper.usuarioAutenticado();
        if (usuario.getOrganizacion() == null)
            throw new BusinessException("Aún no tiene un perfil de organización aprobado");
        return toDTO(usuario.getOrganizacion());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public OrganizacionDTO obtenerPorId(Long id) {
        Organizacion organizacion = organizacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));
        return toDTO(organizacion);
    }

    private OrganizacionDTO toDTO(Organizacion o) {
        OrganizacionDTO dto = new OrganizacionDTO();
        dto.setId(o.getId());
        dto.setRazonSocial(o.getRazonSocial());
        dto.setNit(o.getNit());
        dto.setRepresentanteLegal(o.getRepresentanteLegal());
        dto.setUrlRut(o.getUrlRut());
        dto.setFechaCreacion(o.getFechaCreacion());
        dto.setPromedioRating(o.getPromedioRating());
        dto.setTotalValoraciones(o.getTotalValoraciones());
        dto.setTotalSeguidores(o.getTotalSeguidores());
        dto.setTotalEventosCreados(o.getTotalEventosCreados());
        dto.setEventosFinalizados(o.getEventosFinalizados());
        dto.setEventosRechazados(o.getEventosRechazados());
        dto.setNivel(o.getNivel());
        return dto;
    }
}
