package com.eventhive.app.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.request.AceptarInvitacionRolRequest;
import com.eventhive.app.dto.request.InvitacionRolRequest;
import com.eventhive.app.dto.response.InvitacionRolDTO;
import com.eventhive.app.dto.response.InvitacionRolEnviadaDTO;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.service.ServiceInvitacionRol;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invitaciones/roles")
@RequiredArgsConstructor
public class InvitacionRolApiController {

    private final ServiceInvitacionRol service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','REPRESENTANTE')")
    public ResponseEntity<ApiResponse<Void>> invitar(@Valid @RequestBody InvitacionRolRequest request) {
        service.invitar(request.getCorreo(), request.getRol());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Invitación enviada por correo"));
    }

    @GetMapping("/validar")
    public ResponseEntity<ApiResponse<InvitacionRolDTO>> validar(@RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.ok("Invitación válida", service.validar(token)));
    }

    @GetMapping("/enviadas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','REPRESENTANTE')")
    public ResponseEntity<ApiResponse<PagedResponse<InvitacionRolEnviadaDTO>>> listarEnviadas(Pageable pageable) {
        Page<InvitacionRolEnviadaDTO> page = service.listarEnviadas(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Invitaciones enviadas",
                new PagedResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                        page.getTotalElements(), page.getTotalPages())));
    }

    @PostMapping("/aceptar")
    public ResponseEntity<ApiResponse<Void>> aceptar(@Valid @RequestBody AceptarInvitacionRolRequest request) {
        service.aceptar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Cuenta creada y rol asignado"));
    }
}
