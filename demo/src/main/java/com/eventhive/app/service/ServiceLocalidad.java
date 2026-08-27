package com.eventhive.app.service;

import com.eventhive.app.dto.request.LocalidadRequest;
import com.eventhive.app.enums.PermisoEvento;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Localidad;
import com.eventhive.app.repository.LocalidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceLocalidad {

    private final LocalidadRepository localidadRepository;
    private final ServiceEvento serviceEvento;

    //CONSULTAS
    @Transactional(readOnly = true)
    public List<Localidad> listarPorEvento(Long eventoId) {
        serviceEvento.obtenerReferenciasEvento(eventoId); // validar que el evento exista
        return localidadRepository.findByEventoId(eventoId);
    }

    public Localidad obtenerPorId(Long id) {
        return localidadRepository.findByIdConEvento(id)
                .orElseThrow(() -> new ResourceNotFoundException("Localidad no encontrada con id: " + id));
    }

    //OPERACIONES CRUD
    @Transactional
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
    public Localidad agregar(Long eventoId, LocalidadRequest request) {
        Evento evento = serviceEvento.obtenerReferenciasEvento(eventoId);
        serviceEvento.verificarPermiso(evento, PermisoEvento.EDITAR_EVENTO);

        Localidad localidad = new Localidad();
        localidad.setNombre(request.getNombre());
        localidad.setPrecio(request.getPrecio());
        localidad.setCapacidad(request.getCapacidad());
        localidad.setDisponibles(request.getCapacidad());
        localidad.setEvento(evento);

        return localidadRepository.save(localidad);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
    public Localidad actualizar(Long eventoId, Long localidadId, LocalidadRequest datos) {
        Evento evento = serviceEvento.obtenerReferenciasEvento(eventoId);
        serviceEvento.verificarPermiso(evento, PermisoEvento.EDITAR_EVENTO);

        Localidad localidad = obtenerPorId(localidadId);

        if (!localidad.getEvento().getId().equals(eventoId)) {
            throw new BusinessException("La localidad no pertenece al evento indicado");
        }

        // disponibles se setea segun la direferencia de la capacidad antigua y la nueva
        //para evitar errores con los ya vendidos
        int diferenciaCapacidad = datos.getCapacidad() - localidad.getCapacidad();
        int nuevosDisponibles = localidad.getDisponibles() + diferenciaCapacidad;

        if (nuevosDisponibles < 0) {
            throw new BusinessException(
                    "No es posible reducir la capacidad por debajo de los boletos ya vendidos");
        }

        localidad.setNombre(datos.getNombre());
        localidad.setPrecio(datos.getPrecio());
        localidad.setCapacidad(datos.getCapacidad());
        localidad.setDisponibles(nuevosDisponibles);
        return localidadRepository.save(localidad);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
    public void eliminar(Long eventoId, Long localidadId) {
        Evento evento = serviceEvento.obtenerReferenciasEvento(eventoId);
        serviceEvento.verificarPermiso(evento, PermisoEvento.EDITAR_EVENTO);

        Localidad localidad = obtenerPorId(localidadId);

        if (!localidad.getEvento().getId().equals(eventoId)) {
            throw new BusinessException("La localidad no pertenece al evento indicado");
        }

        localidadRepository.deleteById(localidadId);
    }
}
