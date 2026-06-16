package com.example.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Rol;
import com.example.demo.repository.RolesRepository;
import com.example.demo.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceRoles {

    private final RolesRepository  rolesRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Rol findById(String id) {
        return rolesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public Rol findByNombre(String nombre) {
        return rolesRepository.findByNombre(nombre)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + nombre));
    }

    @Transactional(readOnly = true)
    public Page<Rol> obtenerTodosRoles(Pageable pageable) {
        return rolesRepository.findAll(pageable);
    }

    @Transactional
    public void crearRol(String nombre) {
        if (rolesRepository.existsByNombre(nombre))
            throw new BusinessException("Ya existe un rol con ese nombre");

        Rol nuevo = new Rol();
        nuevo.setNombre(nombre);
        rolesRepository.save(nuevo);
    }

    @Transactional
    public void actualizarRol(String id, String nombre) {
        Rol existente = findById(id);

        if (!existente.getNombre().equalsIgnoreCase(nombre) && rolesRepository.existsByNombre(nombre))
            throw new BusinessException("Ya existe otro rol con ese nombre");

        existente.setNombre(nombre);
        rolesRepository.save(existente);
    }

    @Transactional
    public void eliminarRol(String id) {
        Rol rol = findById(id);

        long usuarios = usuarioRepository.countByRolId(id);
        if (usuarios > 0)
            throw new BusinessException(
                "No se puede eliminar el rol '" + rol.getNombre() + "' porque tiene "
                + usuarios + " usuario(s) asociado(s)");

        rolesRepository.deleteById(id);
    }
}