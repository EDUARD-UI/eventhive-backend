package com.eventhive.app.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.response.UsuarioDTO;
import com.eventhive.app.service.ServiceUsuario;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/moderadores")
@RequiredArgsConstructor
public class ModeradoresApiController {

    private final ServiceUsuario serviceUsuario;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<UsuarioDTO>>> listarModeradores(Pageable pageable) {
        Page<UsuarioDTO> page = serviceUsuario.obtenerModeradoresDTO(pageable);
        PagedResponse<UsuarioDTO> response = new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );

        return ResponseEntity.ok(ApiResponse.ok("Moderadores obtenidos", response));
    }

    @PutMapping("/{id}/revocar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> revocarModerador(@PathVariable Long id) {
        serviceUsuario.revocarModerador(id);
        return ResponseEntity.ok(ApiResponse.ok("Rol de moderador revocado correctamente"));
    }

    @PutMapping("/{id}/asignar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> asignarModerador(@PathVariable Long id) {
        serviceUsuario.asignarModerador(id);
        return ResponseEntity.ok(ApiResponse.ok("Rol de moderador asigando correctamente"));
    }
}
