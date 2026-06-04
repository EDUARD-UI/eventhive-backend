package com.example.demo.controllers;

import com.example.demo.dto.ApiResponse;
import com.example.demo.model.Localidad;
import com.example.demo.service.ServiceLocalidad;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/eventos/{eventoId}/localidades")
@RequiredArgsConstructor
public class LocalidadesApiController {

    private final ServiceLocalidad serviceLocalidad;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Localidad>>> listar(@PathVariable String eventoId) {
        return ResponseEntity.ok(ApiResponse.ok("Localidades obtenidas",
                serviceLocalidad.listarPorEvento(eventoId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Localidad>> agregar(@PathVariable String eventoId,
                                                           @RequestBody Localidad localidad) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Localidad agregada",
                        serviceLocalidad.agregar(eventoId, localidad)));
    }

    @PutMapping("/{localidadId}")
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Localidad>> actualizar(@PathVariable String eventoId,
                                                              @PathVariable String localidadId,
                                                              @RequestBody Localidad localidad) {
        return ResponseEntity.ok(ApiResponse.ok("Localidad actualizada",
                serviceLocalidad.actualizar(eventoId, localidadId, localidad)));
    }

    @DeleteMapping("/{localidadId}")
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable String eventoId,
                                                       @PathVariable String localidadId) {
        serviceLocalidad.eliminar(eventoId, localidadId);
        return ResponseEntity.ok(ApiResponse.ok("Localidad eliminada"));
    }
}