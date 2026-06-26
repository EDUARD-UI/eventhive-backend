package com.eventhive.app.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Localidad;
import com.eventhive.app.repository.LocalidadRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceLocalidad {

    private final LocalidadRepository localidadRepository;
    private final ServiceEvento       serviceEvento;

    @Transactional(readOnly = true)
    public List<Localidad> listarPorEvento(Long eventoId) {
        serviceEvento.obtenerPorId(eventoId); // valida que el evento exista
        return localidadRepository.findByEventoId(eventoId);
    }

    @Transactional(readOnly = true)
    public Localidad obtenerPorId(Long id) {
        return localidadRepository.findByIdConEvento(id)
                .orElseThrow(() -> new ResourceNotFoundException("Localidad no encontrada con id: " + id));
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public Localidad agregar(Long eventoId, Localidad localidad) {
        Evento evento = serviceEvento.obtenerPorId(eventoId);
        serviceEvento.verificarPermiso(evento);

        if (localidad.getCapacidad() <= 0)
            throw new BusinessException("La capacidad debe ser mayor a 0");

        if (localidad.getDisponibles() <= 0)
            localidad.setDisponibles(localidad.getCapacidad());

        localidad.setEvento(evento);
        return localidadRepository.save(localidad);
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public Localidad actualizar(Long eventoId, Long localidadId, Localidad datos) {
        Evento evento = serviceEvento.obtenerPorId(eventoId);
        serviceEvento.verificarPermiso(evento);

        Localidad localidad = obtenerPorId(localidadId);

        if (!localidad.getEvento().getId().equals(eventoId))
            throw new BusinessException("La localidad no pertenece al evento indicado");

        if (datos.getCapacidad() <= 0)
            throw new BusinessException("La capacidad debe ser mayor a 0");

        localidad.setNombre(datos.getNombre());
        localidad.setPrecio(datos.getPrecio());
        localidad.setCapacidad(datos.getCapacidad());
        localidad.setDisponibles(datos.getDisponibles());
        return localidadRepository.save(localidad);
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public void eliminar(Long eventoId, Long localidadId) {
        Evento evento = serviceEvento.obtenerPorId(eventoId);
        serviceEvento.verificarPermiso(evento);

        Localidad localidad = obtenerPorId(localidadId);

        if (!localidad.getEvento().getId().equals(eventoId))
            throw new BusinessException("La localidad no pertenece al evento indicado");

        localidadRepository.deleteById(localidadId);
    }
}