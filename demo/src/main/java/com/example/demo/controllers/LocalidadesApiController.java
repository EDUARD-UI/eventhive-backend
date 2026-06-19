package com.example.demo.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.model.Localidad;
import com.example.demo.service.ServiceLocalidad;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/eventos/{eventoId}/localidades")
@RequiredArgsConstructor
public class LocalidadesApiController {

    private final ServiceLocalidad serviceLocalidad;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Localidad>>> listar(@PathVariable Long eventoId) {
        return ResponseEntity.ok(ApiResponse.ok("Localidades obtenidas",
                serviceLocalidad.listarPorEvento(eventoId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
        public ResponseEntity<ApiResponse<Localidad>> agregar(@PathVariable Long eventoId,
                                                           @RequestBody Localidad localidad) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Localidad agregada",
                serviceLocalidad.agregar(eventoId, localidad)));
    }

    @PutMapping("/{localidadId}")
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Localidad>> actualizar(@PathVariable Long eventoId,
                                                              @PathVariable Long localidadId,
                                                              @RequestBody Localidad localidad) {
        return ResponseEntity.ok(ApiResponse.ok("Localidad actualizada",
                serviceLocalidad.actualizar(eventoId, localidadId, localidad)));
    }

    @DeleteMapping("/{localidadId}")
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long eventoId,
                                                       @PathVariable Long localidadId) {
        serviceLocalidad.eliminar(eventoId, localidadId);
        return ResponseEntity.ok(ApiResponse.ok("Localidad eliminada"));
    }
}