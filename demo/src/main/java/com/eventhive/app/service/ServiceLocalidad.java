package com.eventhive.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.dto.request.LocalidadRequest;
import com.eventhive.app.dto.response.LocalidadDTO;
import com.eventhive.app.enums.PermisoEvento;
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
    private final ServiceEvento serviceEvento;

    //CONSULTAS
    @Transactional(readOnly = true)
    public List<Localidad> listarPorEvento(Long eventoId) {
        Evento evento = serviceEvento.obtenerReferenciasEvento(eventoId);//validar que el evento existe
        serviceEvento.verificarPermiso(evento, PermisoEvento.EDITAR_EVENTO);

        return localidadRepository.findByEventoId(eventoId);
    }

    public Localidad obtenerPorId(Long id) {
        return localidadRepository.findByIdConEvento(id)
                .orElseThrow(() -> new ResourceNotFoundException("Localidad no encontrada con id: " + id));
    }

    //OPERACIONES CRUD
    @Transactional
    public Localidad agregar(Long eventoId, LocalidadRequest request) {
        Evento evento = serviceEvento.obtenerReferenciasEvento(eventoId);
        serviceEvento.verificarPermiso(evento, PermisoEvento.EDITAR_EVENTO);

        validarLocalidadRequest(request);

        long total = localidadRepository.countByEventoId(eventoId);
        String nombre = request.getNombre() == null || request.getNombre().isBlank()
                ? "General"
                : request.getNombre();

        if (total == 0) {
            nombre = "General";
        }

        Localidad localidad = new Localidad();
        localidad.setNombre(nombre);
        localidad.setPrecio(request.getPrecio());
        localidad.setCapacidad(request.getCapacidad());
        localidad.setDisponibles(request.getCapacidad());
        localidad.setEvento(evento);

        return localidadRepository.save(localidad);
    }

    @Transactional
    public Localidad actualizar(Long eventoId, Long localidadId, LocalidadRequest datos) {
        Evento evento = serviceEvento.obtenerReferenciasEvento(eventoId);
        serviceEvento.verificarPermiso(evento, PermisoEvento.EDITAR_EVENTO);

        validarLocalidadRequest(datos);

        Localidad localidad = obtenerPorId(localidadId);

        if (!localidad.getEvento().getId().equals(eventoId)) {
            throw new BusinessException("La localidad no pertenece al evento indicado");
        }

        int diferenciaCapacidad = datos.getCapacidad() - localidad.getCapacidad();
        int nuevosDisponibles = localidad.getDisponibles() + diferenciaCapacidad;

        if (nuevosDisponibles < 0) {
            throw new BusinessException(
                    "No es posible reducir la capacidad por debajo de los boletos ya vendidos");
        }

        if (nuevosDisponibles > datos.getCapacidad()) {
            throw new BusinessException("Los disponibles no pueden superar la capacidad");
        }

        localidad.setNombre(datos.getNombre() == null || datos.getNombre().isBlank() ? "General" : datos.getNombre());
        localidad.setPrecio(datos.getPrecio());
        localidad.setCapacidad(datos.getCapacidad());
        localidad.setDisponibles(nuevosDisponibles);
        return localidadRepository.save(localidad);
    }

    @Transactional
    public void eliminar(Long eventoId, Long localidadId) {
        Evento evento = serviceEvento.obtenerReferenciasEvento(eventoId);
        serviceEvento.verificarPermiso(evento, PermisoEvento.EDITAR_EVENTO);

        Localidad localidad = obtenerPorId(localidadId);

        if (!localidad.getEvento().getId().equals(eventoId)) {
            throw new BusinessException("La localidad no pertenece al evento indicado");
        }

        long total = localidadRepository.countByEventoId(eventoId);
        if (total <= 1) {
            throw new BusinessException("El evento debe mantener al menos una localidad");
        }

        localidadRepository.deleteById(localidadId);
    }

    // METODOS AUXILIARES Y DE MAPEO
    private void validarLocalidadRequest(LocalidadRequest request) {
        if (request == null) {
            throw new BusinessException("La localidad es requerida");
        }
        if (request.getPrecio() == null || request.getPrecio().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El precio debe ser mayor a 0");
        }
        if (request.getCapacidad() == null || request.getCapacidad() <= 0) {
            throw new BusinessException("La capacidad debe ser mayor a 0");
        }
    }

    public LocalidadDTO toDTO(Localidad localidad) {
        LocalidadDTO dto = new LocalidadDTO();
        dto.setId(localidad.getId());
        dto.setNombre(localidad.getNombre());
        dto.setPrecio(localidad.getPrecio());
        dto.setCapacidad(localidad.getCapacidad());
        dto.setDisponibles(localidad.getDisponibles());
        return dto;
    }
}
