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
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Estado;
import com.example.demo.service.ServiceEstado;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/estados")
public class EstadosApiController {

    private final ServiceEstado serviceEstado;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<Estado>>> listarEstados(Pageable pageable) {
        Page<Estado> page = serviceEstado.obtenerEstados(pageable);

        PagedResponse<Estado> response = new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );

        return ResponseEntity.ok(ApiResponse.ok("Estados obtenidos", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Estado>> obtenerEstado(@PathVariable String id) {
        Estado estado = serviceEstado.obtenerEstadoPorId(id);
        if (estado == null) {
            throw new ResourceNotFoundException("Estado no encontrado");
        }
        return ResponseEntity.ok(ApiResponse.ok("Estado obtenido", estado));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> crearEstado(
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion) {

        serviceEstado.crearEstado(nombre, descripcion);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Estado creado exitosamente"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> actualizarEstado(
            @PathVariable String id,
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion) {

        serviceEstado.actualizarEstado(id, nombre, descripcion);
        return ResponseEntity.ok(ApiResponse.ok("Estado actualizado exitosamente"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> eliminarEstado(@PathVariable String id) {
        serviceEstado.eliminarEstado(id);
        return ResponseEntity.ok(ApiResponse.ok("Estado eliminado exitosamente"));
    }
}
