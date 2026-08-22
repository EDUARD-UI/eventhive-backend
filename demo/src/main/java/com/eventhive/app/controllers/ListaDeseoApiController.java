package com.eventhive.app.controllers;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.response.EventoDTO;
import com.eventhive.app.service.ServiceListaDeseo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deseos")
public class ListaDeseoApiController {

    private final ServiceListaDeseo serviceListaDeseo;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> listar(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Eventos deseados obtenidos",
                serviceListaDeseo.listarMisDeseados(pageable)));
    }

    @PostMapping("/{eventoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> agregar(@PathVariable Long eventoId) {
        serviceListaDeseo.agregar(eventoId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Evento agregado a tu lista de deseados"));
    }

    @DeleteMapping("/{eventoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> quitar(@PathVariable Long eventoId) {
        serviceListaDeseo.quitar(eventoId);
        return ResponseEntity.ok(ApiResponse.ok("Evento removido de tu lista de deseados"));
    }
}
