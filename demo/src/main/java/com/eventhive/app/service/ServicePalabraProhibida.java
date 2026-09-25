package com.eventhive.app.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.dto.request.PalabraProhibidaRequest;
import com.eventhive.app.dto.response.PalabraProhibidaDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.PalabraProhibida;
import com.eventhive.app.repository.PalabraProhibidaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServicePalabraProhibida {

    private final PalabraProhibidaRepository repository;

    //CONSULTAS
    @Transactional(readOnly = true)
    public Page<PalabraProhibidaDTO> listar(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDTO);
    }

    //OPERACIONES CRUD
    @Transactional
    public PalabraProhibidaDTO crear(PalabraProhibidaRequest request) {
        String palabraNormalizada = normalizar(request.getPalabra());

        if (repository.existsByPalabraIgnoreCase(palabraNormalizada)) {
            throw new BusinessException("Esa palabra ya está registrada");
        }

        PalabraProhibida entidad = new PalabraProhibida();
        entidad.setPalabra(palabraNormalizada);
        entidad.setSeveridad(request.getSeveridad());
        entidad.setActiva(true);

        return toDTO(repository.save(entidad));
    }

    @Transactional
    public PalabraProhibidaDTO actualizar(Long id, PalabraProhibidaRequest request) {
        PalabraProhibida entidad = obtenerPorId(id);
        String palabraNormalizada = normalizar(request.getPalabra());

        if (!entidad.getPalabra().equalsIgnoreCase(palabraNormalizada)
                && repository.existsByPalabraIgnoreCase(palabraNormalizada)) {
            throw new BusinessException("Ya existe otra palabra registrada con ese texto");
        }

        entidad.setPalabra(palabraNormalizada);
        entidad.setSeveridad(request.getSeveridad());

        return toDTO(repository.save(entidad));
    }

    @Transactional
    public void cambiarActiva(Long id, boolean activa) {
        PalabraProhibida entidad = obtenerPorId(id);
        entidad.setActiva(activa);
        repository.save(entidad);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Palabra prohibida no encontrada");
        }
        repository.deleteById(id);
    }


    //METODOS AUXILIARES Y MAPEO
    private PalabraProhibida obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Palabra prohibida no encontrada"));
    }

    private String normalizar(String palabra) {
        return palabra.trim().toLowerCase();
    }

    private PalabraProhibidaDTO toDTO(PalabraProhibida entidad) {
        PalabraProhibidaDTO dto = new PalabraProhibidaDTO();
        dto.setId(entidad.getId());
        dto.setPalabra(entidad.getPalabra());
        dto.setSeveridad(entidad.getSeveridad());
        dto.setActiva(entidad.isActiva());
        dto.setFechaCreacion(entidad.getFechaCreacion());
        return dto;
    }
}