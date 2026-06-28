package com.eventhive.app.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.SolicitudVerificacionDTO;
import com.eventhive.app.dto.request.SolicitudVerificacionRequest;
import com.eventhive.app.service.ServiceSolicitudVerificacion;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/verificacion")
public class VerificacionApiController {

    private final ServiceSolicitudVerificacion serviceSolicitud;

    @PostMapping(value = "/solicitar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public ResponseEntity<ApiResponse<Void>> crearSolicitud(
            @RequestPart("datos") SolicitudVerificacionRequest request,
            @RequestPart(value = "rut", required = false) MultipartFile archivoRut) {

        serviceSolicitud.crearSolicitud(request, archivoRut);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Solicitud de verificación enviada correctamente"));
    }

    @GetMapping("/mis-solicitudes")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public ResponseEntity<ApiResponse<SolicitudVerificacionDTO>> miSolicitud() {
        return ResponseEntity.ok(ApiResponse.ok("Solicitud obtenida", serviceSolicitud.miSolicitud()));
    }

    @GetMapping("/panel")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Page<SolicitudVerificacionDTO>>> obtenerSolicitudes(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Solicitudes obtenidas",
                serviceSolicitud.obtenerSolicitudesPendientes(pageable)));
    }

    @GetMapping("/{solicitudId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<SolicitudVerificacionDTO>> obtenerDetalles(
            @PathVariable Long solicitudId) {
        return ResponseEntity.ok(ApiResponse.ok("Solicitud obtenida",
            serviceSolicitud.obtenerSolicitud(solicitudId)));
    }

    @PutMapping("/{solicitudId}/aprobar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<String>> aprobarSolicitud(@PathVariable Long solicitudId) {
        String claveGenerada = serviceSolicitud.aprobarSolicitud(solicitudId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Solicitud aprobada. Comparte esta contraseña con el organizador", claveGenerada));
    }

    @PutMapping("/{solicitudId}/rechazar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> rechazarSolicitud(
            @PathVariable Long solicitudId,
            @RequestParam String motivo) {
        serviceSolicitud.rechazarSolicitud(solicitudId, motivo);
        return ResponseEntity.ok(ApiResponse.ok("Solicitud rechazada"));
    }
}
