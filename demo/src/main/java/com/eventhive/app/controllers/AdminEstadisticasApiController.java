package com.eventhive.app.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.response.EventoEstadoConteoDTO;
import com.eventhive.app.dto.response.EventosPorCategoriaDTO;
import com.eventhive.app.dto.response.IngresosPlataformaDTO;
import com.eventhive.app.dto.response.OrganizacionesPorValidacionDTO;
import com.eventhive.app.dto.response.TopEventoVentasDTO;
import com.eventhive.app.dto.response.TopOrganizacionVentasDTO;
import com.eventhive.app.service.ServiceEstadisticasAdmin;

import lombok.RequiredArgsConstructor;

// Métricas del dashboard de administración (solo lectura, agregadas en BD)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/estadisticas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminEstadisticasApiController {

    private final ServiceEstadisticasAdmin serviceEstadisticas;

    //CONSULTAS
    @GetMapping("/organizaciones-por-validacion")
    public ResponseEntity<ApiResponse<OrganizacionesPorValidacionDTO>> organizacionesPorValidacion() {
        return ResponseEntity.ok(ApiResponse.ok("Organizaciones por validación obtenidas",
                serviceEstadisticas.organizacionesPorValidacion()));
    }

    @GetMapping("/eventos-por-estado")
    public ResponseEntity<ApiResponse<List<EventoEstadoConteoDTO>>> eventosPorEstado() {
        return ResponseEntity.ok(ApiResponse.ok("Eventos por estado obtenidos",
                serviceEstadisticas.eventosPorEstado()));
    }

    @GetMapping("/eventos-por-categoria")
    public ResponseEntity<ApiResponse<List<EventosPorCategoriaDTO>>> eventosPorCategoria() {
        return ResponseEntity.ok(ApiResponse.ok("Eventos por categoría obtenidos",
                serviceEstadisticas.eventosPorCategoria()));
    }

    @GetMapping("/top-eventos-ventas")
    public ResponseEntity<ApiResponse<List<TopEventoVentasDTO>>> topEventosPorVentas() {
        return ResponseEntity.ok(ApiResponse.ok("Top 5 eventos por ventas obtenido",
                serviceEstadisticas.topEventosPorVentas()));
    }

    @GetMapping("/top-organizaciones-ventas")
    public ResponseEntity<ApiResponse<List<TopOrganizacionVentasDTO>>> topOrganizacionesPorVentas() {
        return ResponseEntity.ok(ApiResponse.ok("Top 5 organizaciones por ventas obtenido",
                serviceEstadisticas.topOrganizacionesPorVentas()));
    }

    @GetMapping("/ingresos-plataforma")
    public ResponseEntity<ApiResponse<IngresosPlataformaDTO>> ingresosPlataforma() {
        return ResponseEntity.ok(ApiResponse.ok("Ingresos de la plataforma obtenidos",
                serviceEstadisticas.ingresosPlataforma()));
    }
}
