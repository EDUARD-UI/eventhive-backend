package com.eventhive.app.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.request.PlanPromocionRequest;
import com.eventhive.app.dto.response.EventoPromocionadoMarketingDTO;
import com.eventhive.app.dto.response.MarketingEstadisticasDTO;
import com.eventhive.app.dto.response.PlanPromocionDTO;
import com.eventhive.app.service.ServiceMarketing;
import com.eventhive.app.service.SupabaseStorageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/marketing")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MARKETING','ADMINISTRADOR')")
public class MarketingApiController {

    private final ServiceMarketing service;
    private final SupabaseStorageService storageService;

    @GetMapping("/estadisticas")
    public ResponseEntity<ApiResponse<MarketingEstadisticasDTO>> estadisticas() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Estadísticas de marketing obtenidas", service.obtenerEstadisticas()));
    }

    @PostMapping(value = "/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> subirImagen(
            @RequestPart("imagen") MultipartFile imagen) {
        String url = storageService.subirImagenEvento(imagen);
        return ResponseEntity.ok(ApiResponse.ok("Imagen cargada correctamente", Map.of("url", url)));
    }

    @GetMapping("/planes")
    public ResponseEntity<ApiResponse<List<PlanPromocionDTO>>> listarPlanes() {
        return ResponseEntity.ok(ApiResponse.ok("Planes de promoción obtenidos", service.listarPlanes()));
    }

    @PostMapping("/planes")
    public ResponseEntity<ApiResponse<PlanPromocionDTO>> crearPlan(
            @Valid @RequestBody PlanPromocionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Plan de promoción creado correctamente", service.crearPlan(request)));
    }

    @PutMapping("/planes/{id}")
    public ResponseEntity<ApiResponse<PlanPromocionDTO>> actualizarPlan(
            @PathVariable Long id, @Valid @RequestBody PlanPromocionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Plan actualizado", service.actualizarPlan(id, request)));
    }

    @GetMapping("/eventos-promocionados")
    public ResponseEntity<ApiResponse<List<EventoPromocionadoMarketingDTO>>> listarEventosPromocionados() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Eventos con promoción pagada obtenidos", service.listarEventosPromocionados()));
    }
}
