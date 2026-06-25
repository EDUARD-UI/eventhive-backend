package com.example.demo.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.service.ServiceSeguidor;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/organizadores")
public class SeguidorApiController {

    private final ServiceSeguidor serviceSeguidor;

    @PostMapping("/{organizadorId}/seguir")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> seguir(@PathVariable Long organizadorId) {
        serviceSeguidor.seguir(organizadorId);
        return ResponseEntity.ok(ApiResponse.ok("Ahora sigues a este organizador"));
    }

    @DeleteMapping("/{organizadorId}/seguir")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> dejarDeSeguir(@PathVariable Long organizadorId) {
        serviceSeguidor.dejarDeSeguir(organizadorId);
        return ResponseEntity.ok(ApiResponse.ok("Dejaste de seguir al organizador"));
    }

    @GetMapping("/{organizadorId}/seguidores/total")
    public ResponseEntity<ApiResponse<Long>> totalSeguidores(@PathVariable Long organizadorId) {
        return ResponseEntity.ok(ApiResponse.ok("Total seguidores",
                serviceSeguidor.contarSeguidores(organizadorId)));
    }

    @GetMapping("/siguiendo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> listarSiguiendo() {
        return ResponseEntity.ok(ApiResponse.ok("Organizadores seguidos",
                serviceSeguidor.listarSiguiendo()));
    }
}