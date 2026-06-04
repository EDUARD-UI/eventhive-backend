package com.example.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Estado;
import com.example.demo.repository.EstadoRepository;
import com.example.demo.repository.EventoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceEstado {

    private final EstadoRepository estadoRepository;
    private final EventoRepository eventoRepository;

    @Transactional(readOnly = true)
    public Estado findByNombre(String nombre) {
        return estadoRepository.findByNombre(nombre)
                .orElseThrow(() -> new ResourceNotFoundException("Estado no encontrado: " + nombre));
    }

    @Transactional(readOnly = true)
    public Page<Estado> obtenerEstados(Pageable pageable) {
        return estadoRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Estado obtenerEstadoPorId(String id) {
        return estadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estado no encontrado"));
    }

    @Transactional
    public void crearEstado(String nombre, String descripcion) {
        if (estadoRepository.existsByNombre(nombre))
            throw new BusinessException("Ya existe un estado con ese nombre");

        Estado e = new Estado();
        e.setNombre(nombre);
        e.setDescripcion(descripcion);
        estadoRepository.save(e);
    }

    @Transactional
    public void actualizarEstado(String id, String nombre, String descripcion) {
        Estado existente = obtenerEstadoPorId(id);

        if (!existente.getNombre().equalsIgnoreCase(nombre) && estadoRepository.existsByNombre(nombre))
            throw new BusinessException("Ya existe otro estado con ese nombre");

        existente.setNombre(nombre);
        existente.setDescripcion(descripcion);
        estadoRepository.save(existente);
    }

    @Transactional
    public void eliminarEstado(String id) {
        Estado estado = obtenerEstadoPorId(id);
        long eventos = eventoRepository.countByEstadoId(id);

        if (eventos > 0)
            throw new BusinessException(
                "No se puede eliminar '" + estado.getNombre() + "' porque tiene " + eventos + " evento(s)");

        estadoRepository.deleteById(id);
    }
}