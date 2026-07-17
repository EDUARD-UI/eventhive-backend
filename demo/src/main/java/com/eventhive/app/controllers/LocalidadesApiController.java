package com.eventhive.app.controllers;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.request.LocalidadRequest;
import com.eventhive.app.model.Localidad;
import com.eventhive.app.service.ServiceLocalidad;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eventos/{eventoId}/localidades")
@RequiredArgsConstructor
public class LocalidadesApiController {

    private final ServiceLocalidad serviceLocalidad;

    @GetMapping
    @PreAuthorize("hasRole('ORGANIZACION')")
    public ResponseEntity<ApiResponse<List<Localidad>>> listar(@PathVariable Long eventoId) {
        return ResponseEntity.ok(ApiResponse.ok("Localidades obtenidas",
                serviceLocalidad.listarPorEvento(eventoId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZACION')")
    public ResponseEntity<ApiResponse<Localidad>> agregar(@PathVariable Long eventoId,
            @Valid @RequestBody LocalidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Localidad agregada",
                        serviceLocalidad.agregar(eventoId, request)));
    }

    @PutMapping("/{localidadId}")
    @PreAuthorize("hasRole('ORGANIZACION')")
    public ResponseEntity<ApiResponse<Localidad>> actualizar(@PathVariable Long eventoId,
            @PathVariable Long localidadId,
            @Valid @RequestBody LocalidadRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Localidad actualizada",
                serviceLocalidad.actualizar(eventoId, localidadId, request)));
    }

    @DeleteMapping("/{localidadId}")
    @PreAuthorize("hasRole('ORGANIZACION')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long eventoId,
            @PathVariable Long localidadId) {
        serviceLocalidad.eliminar(eventoId, localidadId);
        return ResponseEntity.ok(ApiResponse.ok("Localidad eliminada"));
    }
}
