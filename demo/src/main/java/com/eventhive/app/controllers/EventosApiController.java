package com.eventhive.app.controllers;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.request.EventoRequest;
import com.eventhive.app.dto.request.ModeracionEventoRequest;
import com.eventhive.app.dto.response.EventoBusquedaDTO;
import com.eventhive.app.dto.response.EventoDTO;
import com.eventhive.app.dto.response.EventoMapaDTO;
import com.eventhive.app.service.ServiceEvento;
import com.eventhive.app.service.ServiceModeracion;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
public class EventosApiController {

    private final ServiceEvento serviceEvento;
    private final ServiceModeracion serviceModeracion;
    private final AuthenticatedUserHelper authHelper;

    //CONSULTAS
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> listar(
            @RequestParam(required = false) Long categoriaId,
            Pageable pageable) {

        var page = categoriaId != null
                ? serviceEvento.listarPorCategoria(categoriaId, pageable)
                : serviceEvento.listarTodos(pageable);

        return ResponseEntity.ok(ApiResponse.ok("Eventos obtenidos", serviceEvento.toPagedDTO(page)));
    }

    //eventos cercanos a la fecha actual
    @GetMapping("/proximos")
    public ResponseEntity<ApiResponse<List<EventoDTO>>> eventosProximos(){
        return ResponseEntity.ok(ApiResponse.ok("Proximos eventos obtenidos",
                serviceEvento.listarEventosProximos()));
    }

    // Público: solo eventos PUBLICADOS (bug #2.2)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventoDTO>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Evento obtenido",
                serviceEvento.toDTO(serviceEvento.obtenerEventoPublicoPorId(id))));
    }

    // Organización dueña del evento: cualquier estado, pero solo el propio (bug #2.2)
    @GetMapping("/organizador/{id}")
    public ResponseEntity<ApiResponse<EventoDTO>> obtenerDeMiOrganizacion(@PathVariable Long id) {
        Long organizacionId = authHelper.usuarioAutenticado().getOrganizacion().getId();
        return ResponseEntity.ok(ApiResponse.ok("Evento obtenido",
                serviceEvento.toDTO(serviceEvento.obtenerEventoDeOrganizacionPorId(organizacionId, id))));
    }

    // Administración: cualquier estado (bug #2.2)
    @GetMapping("/admin/{id}")
    public ResponseEntity<ApiResponse<EventoDTO>> obtenerAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Evento obtenido",
                serviceEvento.toDTO(serviceEvento.obtenerEventoAdminPorId(id))));
    }

    @GetMapping("/mapa")
    public ResponseEntity<ApiResponse<List<EventoMapaDTO>>> eventosParaMapa(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radioKm) {

        return ResponseEntity.ok(ApiResponse.ok("Eventos para el mapa",
                serviceEvento.buscarParaMapa(categoriaId, lat, lng, radioKm)));
    }

    @GetMapping("/buscar")
    public ResponseEntity<ApiResponse<PagedResponse<EventoBusquedaDTO>>> buscar(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Pageable pageable) {

        if ((titulo == null || titulo.isBlank()) && fecha == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Debe indicar al menos 'titulo' o 'fecha'"));
        }

        var page = serviceEvento.buscarEventos(titulo, fecha, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Resultados de búsqueda",
                new PagedResponse<>(page.getContent(), page.getNumber(),
                        page.getSize(), page.getTotalElements(), page.getTotalPages())));
    }

    @GetMapping("/organizador")
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> eventosPorOrganizacion(Pageable pageable) {
        Long organizacionId = authHelper.usuarioAutenticado().getOrganizacion().getId();
        return ResponseEntity.ok(ApiResponse.ok("Eventos obtenidos",
                serviceEvento.toPagedDTO(serviceEvento.listarPorOrganizacion(organizacionId, pageable))));
    }

    @GetMapping("/organizador/buscar")
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> filtrarMisEventos(
            @RequestParam String titulo,
            Pageable pageable) {

        if (titulo == null || titulo.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El parámetro 'titulo' es requerido"));
        }

        Long organizacionId = authHelper.usuarioAutenticado().getOrganizacion().getId();
        return ResponseEntity.ok(ApiResponse.ok("Resultados de búsqueda",
                serviceEvento.toPagedDTO(
                        serviceEvento.filtrarPorOrganizacionYTitulo(organizacionId, titulo, pageable))));
    }

    @GetMapping("/admin/buscar")
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> filtrarEventosCRUD(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String estado,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok("Resultados de búsqueda",
                serviceEvento.toPagedDTO(
                        serviceEvento.filtrarCrud(titulo, categoriaId, estado, pageable))));
    }

    //OPERACIONES CRUD
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EventoDTO>> crear(
            @RequestPart("datos") @Valid EventoRequest request,
            @RequestPart(value = "foto", required = false) MultipartFile foto){

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Evento creado",
                        serviceEvento.toDTO(serviceEvento.crearEvento(request, foto))));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EventoDTO>> actualizar(
            @PathVariable Long id,
            @RequestPart("datos") @Valid EventoRequest request,
            @RequestPart(value = "foto", required = false) MultipartFile foto) {

        return ResponseEntity.ok(ApiResponse.ok("Evento actualizado",
                serviceEvento.toDTO(serviceEvento.actualizarEvento(id, request, foto))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        serviceEvento.eliminarEvento(id);
        return ResponseEntity.ok(ApiResponse.ok("Evento eliminado"));
    }
}
