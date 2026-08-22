package com.eventhive.app.controllers;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.enums.*;
import com.eventhive.app.service.ServiceEstados;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/enums")
@RequiredArgsConstructor
public class EstadosApiController {

    private final ServiceEstados serviceEstados;

    @GetMapping("/motivos-rechazos")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('MODERADOR')")
    public ResponseEntity<ApiResponse<List<MotivosRechazos>>> listarMotivosRechazos() {
        return ResponseEntity.ok(ApiResponse.ok("Motivos de rechazos obtenidos", serviceEstados.findMotivosRechazos()));
    }
}
