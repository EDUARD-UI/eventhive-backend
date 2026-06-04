package com.example.demo.controllers;

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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.SolicitudVerificacionDTO;
import com.example.demo.service.ServiceSolicitudVerificacion;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/verificacion")
public class VerificacionApiController {

    private final ServiceSolicitudVerificacion serviceSolicitud;

    @PostMapping(value = "/solicitar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public ResponseEntity<ApiResponse<Void>> crearSolicitud(
            @RequestParam String mensaje,
            @RequestParam(required = false) MultipartFile archivo) {
        serviceSolicitud.crearSolicitud(mensaje, archivo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Solicitud de verificación enviada. Await admin response"));
    }

    @GetMapping("/mis-solicitudes")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public ResponseEntity<ApiResponse<SolicitudVerificacionDTO>> miSolicitud() {
        SolicitudVerificacionDTO dto = serviceSolicitud.miSolicitud();
        return ResponseEntity.ok(ApiResponse.ok("Solicitud obtenida", dto));
    }

    @GetMapping("/panel")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Page<SolicitudVerificacionDTO>>> obtenerSolicitudes(Pageable pageable) {
        Page<SolicitudVerificacionDTO> solicitudes = serviceSolicitud.obtenerSolicitudesPendientes(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Solicitudes obtenidas", solicitudes));
    }

    @GetMapping("/{solicitudId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<SolicitudVerificacionDTO>> obtenerDetalles(
            @PathVariable String solicitudId) {
        SolicitudVerificacionDTO dto = serviceSolicitud.obtenerSolicitud(solicitudId);
        return ResponseEntity.ok(ApiResponse.ok("Solicitud obtenida", dto));
    }

    @PutMapping("/{solicitudId}/aprobar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> aprobarSolicitud(
            @PathVariable String solicitudId) {
        serviceSolicitud.aprobarSolicitud(solicitudId);
        return ResponseEntity.ok(ApiResponse.ok("Solicitud aprobada. Organizador verificado"));
    }

    @PutMapping("/{solicitudId}/rechazar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> rechazarSolicitud(
            @PathVariable String solicitudId,
            @RequestParam String motivo) {
        serviceSolicitud.rechazarSolicitud(solicitudId, motivo);
        return ResponseEntity.ok(ApiResponse.ok("Solicitud rechazada"));
    }
}