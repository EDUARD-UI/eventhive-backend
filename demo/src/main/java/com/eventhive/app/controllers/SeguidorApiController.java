package com.eventhive.app.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.service.ServiceSeguidor;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/seguidores")
public class SeguidorApiController {

    private final ServiceSeguidor serviceSeguidor;

    @PostMapping("/{organizacionId}/seguir")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> seguir(@PathVariable Long organizacionId) {
        serviceSeguidor.seguir(organizacionId);
        return ResponseEntity.ok(ApiResponse.ok("Ahora sigues a este organizador"));
    }

    @DeleteMapping("/{organizacionId}/seguir")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> dejarDeSeguir(@PathVariable Long organizacionId) {
        serviceSeguidor.dejarDeSeguir(organizacionId);
        return ResponseEntity.ok(ApiResponse.ok("Dejaste de seguir al organizador"));
    }
}