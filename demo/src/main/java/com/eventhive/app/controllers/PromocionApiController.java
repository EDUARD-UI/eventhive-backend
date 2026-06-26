package com.eventhive.app.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.PromocionDTO;
import com.eventhive.app.model.Promocion;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.service.ServicePromocion;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/promociones")
public class PromocionApiController {

    private final ServicePromocion servicePromocion;
    private final AuthenticatedUserHelper authHelper;

    // ── Endpoint público: promoción vigente de un evento ──────────────────────
    // No requiere autenticación — lo consulta InfoEvento y PaymentFlow
    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<ApiResponse<PromocionDTO>> porEvento(@PathVariable Long eventoId) {
        try {
            List<Promocion> promociones = servicePromocion.obtenerPorEvento(eventoId);

            if (promociones == null || promociones.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.ok("Sin promoción", null));
            }

            LocalDate hoy = LocalDate.now();
            Promocion vigente = promociones.stream()
                .filter(p -> p.getFechaInicio() != null && p.getFechaFin() != null
                    && !hoy.isBefore(p.getFechaInicio())
                    && !hoy.isAfter(p.getFechaFin()))
                .findFirst()
                .orElse(null);

            if (vigente == null) {
                return ResponseEntity.ok(ApiResponse.ok("Sin promoción vigente", null));
            }

            PromocionDTO dto = new PromocionDTO();
            dto.setId(vigente.getId());
            dto.setDescripcion(vigente.getDescripcion());
            dto.setDescuento(BigDecimal.valueOf(vigente.getDescuento()));
            dto.setFechaInicio(vigente.getFechaInicio());
            dto.setFechaFinal(vigente.getFechaFin());

            return ResponseEntity.ok(ApiResponse.ok("Promoción obtenida", dto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al obtener promoción: " + e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<PromocionDTO>>> listarTodas(Pageable pageable) {
        Page<PromocionDTO> page = servicePromocion.obtenerTodasPromociones(pageable);
        PagedResponse<PromocionDTO> response = new PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
        return ResponseEntity.ok(ApiResponse.ok("Promociones obtenidas", response));
    }

    @GetMapping("/organizador")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<PromocionDTO>>> porOrganizador(Pageable pageable) {
        Usuario usuario = authHelper.usuarioAutenticado();
        Page<PromocionDTO> page = servicePromocion.obtenerDTOPorOrganizador(usuario.getId(), pageable);
        PagedResponse<PromocionDTO> response = new PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
        return ResponseEntity.ok(ApiResponse.ok("Promociones obtenidas", response));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> crear(
            @RequestParam Long eventoId,
            @RequestParam String descripcion,
            @RequestParam BigDecimal descuento,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin) {
        Usuario usuario = authHelper.usuarioAutenticado();
        servicePromocion.crearPromocion(eventoId, descripcion, descuento, fechaInicio, fechaFin, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Promoción creada"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> actualizar(
            @PathVariable Long id,
            @RequestParam Long eventoId,
            @RequestParam String descripcion,
            @RequestParam BigDecimal descuento,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin) {
        Usuario usuario = authHelper.usuarioAutenticado();
        servicePromocion.actualizarPromocion(id, eventoId, descripcion, descuento, fechaInicio, fechaFin, usuario);
        return ResponseEntity.ok(ApiResponse.ok("Promoción actualizada"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        Usuario usuario = authHelper.usuarioAutenticado();
        servicePromocion.eliminarPromocion(id, usuario);
        return ResponseEntity.ok(ApiResponse.ok("Promoción eliminada"));
    }
}
