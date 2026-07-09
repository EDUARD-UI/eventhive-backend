package com.eventhive.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.repository.OrganizacionRepository;
import com.eventhive.app.repository.SeguidorRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.repository.ValoracionRepository;

import lombok.RequiredArgsConstructor;

//service creado para evitar hacer querys cada q se consulte el perifl de un organizador
//se actualizan los datos cada q haya un cambio en las valoraciones, seguidores o eventos creados por el organizador

@Service
@RequiredArgsConstructor
public class ServiceMetricasOrganizacion {

    private final UsuarioRepository      usuarioRepository;
    private final OrganizacionRepository organizacionRepository;
    private final ValoracionRepository   valoracionRepository;
    private final SeguidorRepository     seguidorRepository;
    private final EventoRepository       eventoRepository;

    @Transactional
    public void actualizarMetricasValoracion(Long organizadorId) {
        Organizacion organizacion = obtenerOrganizacion(organizadorId);

        int total = (int) valoracionRepository.countByOrganizadorId(organizadorId);
        double promedio = total > 0
                ? valoracionRepository.calcularPromedioByOrganizadorId(organizadorId)
                : 0.0;

        organizacion.setTotalValoraciones(total);
        organizacion.setPromedioRating(Math.round(promedio * 10.0) / 10.0);
        organizacionRepository.save(organizacion);
    }

    @Transactional
    public void actualizarTotalSeguidores(Long organizadorId) {
        Organizacion organizacion = obtenerOrganizacion(organizadorId);
        int total = (int) seguidorRepository.countByOrganizadorId(organizadorId);
        organizacion.setTotalSeguidores(total);
        organizacionRepository.save(organizacion);
    }

    @Transactional
    public void actualizarTotalEventos(Long organizadorId) {
        Organizacion organizacion = obtenerOrganizacion(organizadorId);
        int total = (int) eventoRepository.countByOrganizadorId(organizadorId);
        organizacion.setTotalEventosCreados(total);
        organizacionRepository.save(organizacion);
    }

    // Obtiene la Organizacion asociada a un organizador; falla si aún no fue verificado
    private Organizacion obtenerOrganizacion(Long organizadorId) {
        Usuario organizador = usuarioRepository.findById(organizadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizador no encontrado"));
        if (organizador.getOrganizacion() == null)
            throw new BusinessException("El organizador aún no tiene un perfil de organización verificado");
        return organizador.getOrganizacion();
    }
}