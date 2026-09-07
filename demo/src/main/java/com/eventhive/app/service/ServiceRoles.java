package com.eventhive.app.service;

import com.eventhive.app.dto.response.RolDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Rol;
import com.eventhive.app.repository.RolesRepository;
import com.eventhive.app.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServiceRoles {

    private final RolesRepository rolesRepository;
    private final UsuarioRepository usuarioRepository;

    private static final Set<String> ROLES_SISTEMA = Set.of(
            "ADMINISTRADOR", "MODERADOR", "REPRESENTANTE", "OPERADOR", "CLIENTE"
    );

    //CONSULTAS
    @Transactional(readOnly = true)
    public Rol findById(Long id) {
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

    //OPERACION CRUD
    @Transactional
    public void crearRol(String nombre) {
        if (rolesRepository.existsByNombre(nombre))
            throw new BusinessException("Ya existe un rol con ese nombre");

        Rol nuevo = new Rol();
        nuevo.setNombre(nombre);
        rolesRepository.save(nuevo);
    }

    @Transactional
    public void actualizarRol(Long rolId, String nombre) {
        Rol existente = findById(rolId);

        if (ROLES_SISTEMA.contains(existente.getNombre().toUpperCase())) {
            throw new BusinessException(
                    "El rol '" + existente.getNombre() + "' es un rol del sistema y no puede modificarse");
        }

        if (!existente.getNombre().equalsIgnoreCase(nombre) && rolesRepository.existsByNombre(nombre))
            throw new BusinessException("Ya existe otro rol con ese nombre");

        existente.setNombre(nombre);
        rolesRepository.save(existente);
    }

    @Transactional
    public void eliminarRol(Long rolId) {
        Rol rol = findById(rolId);

        if (ROLES_SISTEMA.contains(rol.getNombre().toUpperCase())) {
            throw new BusinessException(
                    "El rol '" + rol.getNombre() + "' es un rol del sistema y no puede eliminarse");
        }

        long usuarios = usuarioRepository.countByRolId(rolId);
        if (usuarios > 0)
            throw new BusinessException(
                    "No se puede eliminar el rol '" + rol.getNombre() + "' porque tiene "
                            + usuarios + " usuario(s) asociado(s)");
        rolesRepository.deleteById(rolId);
    }

    //METODOS AUXILIARES Y DE MAPEO
    public RolDTO toDTO(Rol r) {
        RolDTO dto = new RolDTO();
        dto.setId(r.getId());
        dto.setNombre(r.getNombre());
        return dto;
    }
}