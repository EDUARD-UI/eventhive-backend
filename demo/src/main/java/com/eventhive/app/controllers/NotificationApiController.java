package com.eventhive.app.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.response.NotificationDTO;
import com.eventhive.app.service.ServiceNotification;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notificaciones")
public class NotificationApiController {

    private final ServiceNotification serviceNotification;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("Notificaciones obtenidas",
                serviceNotification.obtenerMisNotificaciones()));
    }

    @GetMapping("/no-leidas")
    public ResponseEntity<ApiResponse<Long>> contarNoLeidas() {
        return ResponseEntity.ok(ApiResponse.ok("Total no leídas",
                serviceNotification.contarNoLeidas()));
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<ApiResponse<Void>> marcarLeida(@PathVariable String id) {
        serviceNotification.marcarLeida(id);
        return ResponseEntity.ok(ApiResponse.ok("Notificación marcada como leída"));
    }

    @DeleteMapping("/limpiar")
    public ResponseEntity<ApiResponse<Void>> limpiarLeidas() {
        serviceNotification.limpiarLeidas();
        return ResponseEntity.ok(ApiResponse.ok("Notificaciones leídas eliminadas"));
    }
}