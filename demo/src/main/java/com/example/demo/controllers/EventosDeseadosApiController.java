package com.example.demo.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.service.ServiceEventosDeseados;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/eventos/{eventoId}/deseados")
@RequiredArgsConstructor
public class EventosDeseadosApiController {

    private final ServiceEventosDeseados serviceDeseados;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> agregar(@PathVariable String eventoId) {
        serviceDeseados.agregar(eventoId);
        return ResponseEntity.ok(ApiResponse.ok("Evento agregado a favoritos"));
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable String eventoId) {
        serviceDeseados.eliminar(eventoId);
        return ResponseEntity.ok(ApiResponse.ok("Evento eliminado de favoritos"));
    }
}