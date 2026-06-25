package com.example.demo.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.NotificationDTO;
import com.example.demo.service.ServiceNotification;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notificaciones")
public class NotificationApiController {

    private final ServiceNotification serviceNotification;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("Notificaciones obtenidas",
                serviceNotification.obtenerMisNotificaciones()));
    }

    @GetMapping("/no-leidas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Long>> contarNoLeidas() {
        return ResponseEntity.ok(ApiResponse.ok("Total no leídas",
                serviceNotification.contarNoLeidas()));
    }

    @PutMapping("/{id}/leer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> marcarLeida(@PathVariable String id) {
        serviceNotification.marcarLeida(id);
        return ResponseEntity.ok(ApiResponse.ok("Notificación marcada como leída"));
    }

    @DeleteMapping("/limpiar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> limpiarLeidas() {
        serviceNotification.limpiarLeidas();
        return ResponseEntity.ok(ApiResponse.ok("Notificaciones leídas eliminadas"));
    }
}