package com.eventhive.app.controllers;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.eventhive.app.dto.EventoBusquedaDTO;
import com.eventhive.app.dto.EventoDTO;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.request.EventoRequest;
import com.eventhive.app.service.ServiceEvento;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
public class EventosApiController {

    private final ServiceEvento serviceEvento;
    private final AuthenticatedUserHelper authHelper;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> listar(
            @RequestParam(required = false) Long categoriaId,
            Pageable pageable) {

        var page = categoriaId != null
                ? serviceEvento.listarPorCategoria(categoriaId, pageable)
                : serviceEvento.listarTodos(pageable);

        return ResponseEntity.ok(ApiResponse.ok("Eventos obtenidos", serviceEvento.toPagedDTO(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventoDTO>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Evento obtenido", serviceEvento.toDTO(serviceEvento.obtenerPorId(id))));
    }

    @GetMapping("/buscar")
    public ResponseEntity<ApiResponse<PagedResponse<EventoBusquedaDTO>>> buscar(
            @RequestParam String titulo,
            Pageable pageable) {

        if (titulo == null || titulo.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El parámetro 'titulo' es requerido"));
        }

        var page = serviceEvento.buscarPorTitulo(titulo, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Resultados de búsqueda",
                new PagedResponse<>(page.getContent(), page.getNumber(),
                        page.getSize(), page.getTotalElements(), page.getTotalPages())));
    }

    @GetMapping("/organizador")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> eventosPorOrganzador(Pageable pageable) {
        Long id = authHelper.usuarioAutenticado().getId();
        return ResponseEntity.ok(ApiResponse.ok("Eventos obtenidos",
                serviceEvento.toPagedDTO(serviceEvento.listarPorOrganizador(id, pageable))));
    }

    @GetMapping("/organizador/buscar")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> buscarMisEventos(
            @RequestParam String titulo,
            Pageable pageable) {

        if (titulo == null || titulo.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El parámetro 'titulo' es requerido"));
        }

        Long id = authHelper.usuarioAutenticado().getId();
        return ResponseEntity.ok(ApiResponse.ok("Resultados de búsqueda",
                serviceEvento.toPagedDTO(
                        serviceEvento.buscarPorOrganizadorYTitulo(id, titulo, pageable))));
    }

    @GetMapping("/admin/buscar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> filtrarEventosAdmin(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String estado,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok("Resultados de búsqueda",
                serviceEvento.toPagedDTO(
                        serviceEvento.buscarAdmin(titulo, categoriaId, estado, pageable))));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<EventoDTO>> crear(
            @RequestPart("datos") EventoRequest request,
            @RequestPart(value = "foto", required = false) MultipartFile foto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Evento creado",
                        serviceEvento.toDTO(serviceEvento.crearEvento(request, foto))));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<EventoDTO>> actualizar(
            @PathVariable Long id,
            @RequestPart("datos") EventoRequest request,
            @RequestPart(value = "foto", required = false) MultipartFile foto) {

        return ResponseEntity.ok(ApiResponse.ok("Evento actualizado",
                serviceEvento.toDTO(serviceEvento.actualizarEvento(id, request, foto))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        serviceEvento.eliminarEvento(id);
        return ResponseEntity.ok(ApiResponse.ok("Evento eliminado"));
    }
}
