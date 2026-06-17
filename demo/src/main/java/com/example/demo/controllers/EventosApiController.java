package com.example.demo.controllers;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.EventoBusquedaDTO;
import com.example.demo.dto.EventoDTO;
import com.example.demo.dto.PagedResponse;
import com.example.demo.model.Evento;
import com.example.demo.service.ServiceEvento;
import com.example.demo.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
public class EventosApiController {

    private final ServiceEvento           serviceEvento;
    private final AuthenticatedUserHelper authHelper;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> listar(
            @RequestParam(required = false) String categoriaId,
            Pageable pageable) {

        var page = categoriaId != null && !categoriaId.isBlank()
                ? serviceEvento.listarPorCategoria(categoriaId, pageable)
                : serviceEvento.listarTodos(pageable);

        return ResponseEntity.ok(ApiResponse.ok("Eventos obtenidos", serviceEvento.toPagedDTO(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventoDTO>> obtener(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Evento obtenido",
                serviceEvento.toDTO(serviceEvento.obtenerPorId(id))));
    }

    @GetMapping("/buscar")
    public ResponseEntity<ApiResponse<PagedResponse<EventoBusquedaDTO>>> buscar(
            @RequestParam String titulo,
            Pageable pageable) {

        if (titulo == null || titulo.isBlank())
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El parámetro 'titulo' es requerido"));

        var page = serviceEvento.buscarPorTitulo(titulo, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Resultados de búsqueda",
                new PagedResponse<>(page.getContent(), page.getNumber(),
                        page.getSize(), page.getTotalElements(), page.getTotalPages())));
    }

    @GetMapping("/organizador")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> listarMisEventos(Pageable pageable) {
        String id = authHelper.usuarioAutenticado().getId();
        return ResponseEntity.ok(ApiResponse.ok("Eventos obtenidos",
                serviceEvento.toPagedDTO(serviceEvento.listarPorOrganizador(id, pageable))));
    }

    @GetMapping("/organizador/buscar")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> buscarMisEventos(
            @RequestParam String titulo,
            Pageable pageable) {

        if (titulo == null || titulo.isBlank())
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El parámetro 'titulo' es requerido"));

        String id = authHelper.usuarioAutenticado().getId();
        return ResponseEntity.ok(ApiResponse.ok("Resultados de búsqueda",
                serviceEvento.toPagedDTO(
                        serviceEvento.buscarPorOrganizadorYTitulo(id, titulo, pageable))));
    }

    @GetMapping("/admin/buscar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> buscarAdmin(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String categoriaId,
            @RequestParam(required = false) String estado,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok("Resultados de búsqueda",
                serviceEvento.toPagedDTO(
                        serviceEvento.buscarAdmin(titulo, categoriaId, estado, pageable))));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<EventoDTO>> crear(@RequestBody Evento evento) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Evento creado",
                        serviceEvento.toDTO(serviceEvento.crearEvento(evento))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<EventoDTO>> actualizar(
            @PathVariable String id,
            @RequestBody Evento evento) {

        return ResponseEntity.ok(ApiResponse.ok("Evento actualizado",
                serviceEvento.toDTO(serviceEvento.actualizarEvento(id, evento))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable String id) {
        serviceEvento.eliminarEvento(id);
        return ResponseEntity.ok(ApiResponse.ok("Evento eliminado"));
    }
}