package com.eventhive.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Rol;
import com.eventhive.app.repository.RolesRepository;
import com.eventhive.app.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RolesServiceTest {

    @Mock
    private RolesRepository rolesRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ServiceRoles serviceRoles; // Mockito inyecta los dos repositorios aquí automáticamente

    // ==========================================
    // PRUEBAS PARA: findById(Long id)
    // ==========================================

    @Test
    public void findById_DebeRetornarRol_CuandoElIdExiste() {
        // Arrange
        Long id = 1L;
        Rol rolFalso = new Rol();
        rolFalso.setId(id);
        rolFalso.setNombre("ADMIN");

        when(rolesRepository.findById(id)).thenReturn(Optional.of(rolFalso));

        // Act
        Rol resultado = serviceRoles.findById(id);

        // Assert
        assertNotNull(resultado);
        assertEquals("ADMIN", resultado.getNombre());
        assertEquals(id, resultado.getId());
        verify(rolesRepository, times(1)).findById(id); // Verifica que se llamó al repositorio exactamente 1 vez
    }

    @Test
    public void findById_DebeLanzarResourceNotFoundException_CuandoElIdNoExiste() {
        // Arrange
        Long id = 99L;
        when(rolesRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            serviceRoles.findById(id);
        }, "Se esperaba ResourceNotFoundException debido a que el ID no existe");
    }

    // ==========================================
    // PRUEBAS PARA: crearRol(String nombre)
    // ==========================================

    @Test
    public void crearRol_DebeGuardarRol_CuandoElNombreNoExiste() {
        // Arrange
        String nombreRol = "INVITADO";
        when(rolesRepository.existsByNombre(nombreRol)).thenReturn(false);

        // Act
        serviceRoles.crearRol(nombreRol);

        // Assert
        // Verificamos que se haya ejecutado el método .save() con cualquier objeto de tipo Rol
        verify(rolesRepository, times(1)).save(any(Rol.class));
    }

    @Test
    public void crearRol_DebeLanzarBusinessException_CuandoElNombreYaExiste() {
        // Arrange
        String nombreRol = "ADMIN";
        when(rolesRepository.existsByNombre(nombreRol)).thenReturn(true);

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            serviceRoles.crearRol(nombreRol);
        }, "Se esperaba BusinessException porque el rol ya está duplicado");

        // Verificamos que NUNCA se haya llamado al método .save() tras lanzar la excepción
        verify(rolesRepository, never()).save(any(Rol.class));
    }

    // ==========================================
    // PRUEBAS PARA: eliminarRol(Long id)
    // ==========================================

    @Test
    public void eliminarRol_DebeEliminar_CuandoNoTieneUsuariosAsociados() {
        // Arrange
        Long id = 1L;
        Rol rolExistente = new Rol();
        rolExistente.setId(id);
        rolExistente.setNombre("MODERADOR");

        // Primero debe pasar la validación interna del findById
        when(rolesRepository.findById(id)).thenReturn(Optional.of(rolExistente));
        // Simulamos que el conteo de usuarios asociados da cero (0)
        when(usuarioRepository.countByRolId(id)).thenReturn(0L);

        // Act
        serviceRoles.eliminarRol(id);

        // Assert
        verify(rolesRepository, times(1)).deleteById(id);
    }

    @Test
    public void eliminarRol_DebeLanzarBusinessException_CuandoTieneUsuariosAsociados() {
        // Arrange
        Long id = 1L;
        Rol rolExistente = new Rol();
        rolExistente.setId(id);
        rolExistente.setNombre("MODERADOR");

        when(rolesRepository.findById(id)).thenReturn(Optional.of(rolExistente));
        // Simulamos que hay 5 usuarios usando este rol actualmente
        when(usuarioRepository.countByRolId(id)).thenReturn(5L);

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            serviceRoles.eliminarRol(id);
        }, "Se esperaba BusinessException porque el rol tiene usuarios asociados");

        // Verificamos que el borrado NUNCA se ejecute para proteger la integridad del sistema
        verify(rolesRepository, never()).deleteById(id);
    }
}
