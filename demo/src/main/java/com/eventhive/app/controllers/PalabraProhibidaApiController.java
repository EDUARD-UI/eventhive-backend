package com.eventhive.app.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.request.PalabraProhibidaRequest;
import com.eventhive.app.dto.response.PalabraProhibidaDTO;
import com.eventhive.app.service.ServicePalabraProhibida;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/administracion/palabras-prohibidas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class PalabraProhibidaApiController {

    private final ServicePalabraProhibida service;

    //CONSULTAS
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PalabraProhibidaDTO>>> listar(Pageable pageable) {
        Page<PalabraProhibidaDTO> page = service.listar(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Palabras prohibidas obtenidas",
                new PagedResponse<>(
                        page.getContent(),
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages())));
    }

    //OPERCIONES CRUD
    @PostMapping
    public ResponseEntity<ApiResponse<PalabraProhibidaDTO>> crear(
            @Valid @RequestBody PalabraProhibidaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Palabra creada", service.crear(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PalabraProhibidaDTO>> actualizar(
            @PathVariable Long id, @Valid @RequestBody PalabraProhibidaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Palabra actualizada", service.actualizar(id, request)));
    }

    @PatchMapping("/{id}/activa")
    public ResponseEntity<ApiResponse<Void>> cambiarActiva(
            @PathVariable Long id, @RequestParam boolean activa) {
        service.cambiarActiva(id, activa);
        return ResponseEntity.ok(ApiResponse.ok(activa ? "Palabra activada" : "Palabra desactivada"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Palabra eliminada"));
    }
}
