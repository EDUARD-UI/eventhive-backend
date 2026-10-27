package com.eventhive.app.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.response.OrganizacionPublicaDTO;
import com.eventhive.app.service.ServiceSeguidor;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/seguidores")
public class SeguidorApiController {

    private final ServiceSeguidor serviceSeguidor;

    @GetMapping({"/mis-seguidos", "/siguiendo"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<OrganizacionPublicaDTO>>> listarOrganizacionesSeguidas(Pageable pageable) {
        Page<OrganizacionPublicaDTO> page = serviceSeguidor.listarOrganizacionesSeguidas(pageable);
        PagedResponse<OrganizacionPublicaDTO> response = new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
        return ResponseEntity.ok(ApiResponse.ok("Organizaciones seguidas obtenidas", response));
    }

    @GetMapping("/{organizacionId}/siguiendo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Boolean>> esSeguidor(@PathVariable Long organizacionId) {
        return ResponseEntity.ok(ApiResponse.ok("Estado de seguimiento", serviceSeguidor.esSeguidor(organizacionId)));
    }

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