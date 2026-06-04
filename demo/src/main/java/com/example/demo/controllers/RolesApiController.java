package com.example.demo.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.PagedResponse;
import com.example.demo.model.Rol;
import com.example.demo.service.ServiceRoles;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/roles")
public class RolesApiController {

    private final ServiceRoles serviceRoles;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<Rol>>> listarRoles(Pageable pageable) {
        Page<Rol> page = serviceRoles.obtenerTodosRoles(pageable);
        PagedResponse<Rol> response = new PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
        return ResponseEntity.ok(ApiResponse.ok("Roles obtenidos", response));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> crearRol(
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion) {

        serviceRoles.crearRol(nombre, descripcion);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Rol creado exitosamente"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> actualizarRol(
            @PathVariable String id,
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion) {

        serviceRoles.actualizarRol(id, nombre, descripcion);
        return ResponseEntity.ok(ApiResponse.ok("Rol actualizado exitosamente"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> eliminarRol(@PathVariable String id) {
        serviceRoles.eliminarRol(id);
        return ResponseEntity.ok(ApiResponse.ok("Rol eliminado exitosamente"));
    }
}
