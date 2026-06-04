package com.example.demo.controllers;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    private final ServiceEvento       serviceEvento;
    private final AuthenticatedUserHelper authHelper;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> listar(
            @RequestParam(required = false) String categoriaId, Pageable pageable) {
        Page<Evento> page = categoriaId != null && !categoriaId.isBlank()
                ? serviceEvento.listarPorCategoria(categoriaId, pageable)
                : serviceEvento.listarTodos(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Eventos obtenidos", toPagedDTO(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventoDTO>> obtener(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Evento obtenido", toDTO(serviceEvento.obtenerPorId(id))));
    }

    @GetMapping("/buscar")
    public ResponseEntity<ApiResponse<PagedResponse<EventoBusquedaDTO>>> buscar(
            @RequestParam String titulo, Pageable pageable) {
        if (titulo == null || titulo.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.error("El parámetro 'titulo' es requerido"));
        Page<EventoBusquedaDTO> page = serviceEvento.buscarPorTitulo(titulo, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Resultados de búsqueda",
                new PagedResponse<>(page.getContent(), page.getNumber(),
                        page.getSize(), page.getTotalElements(), page.getTotalPages())));
    }

    @GetMapping("/organizador")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> listarMisEventos(Pageable pageable) {
        String id = authHelper.usuarioAutenticado().getId();
        Page<Evento> page = serviceEvento.listarPorOrganizador(id, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Eventos obtenidos", toPagedDTO(page)));
    }

    @GetMapping("/organizador/buscar")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> buscarMisEventos(
            @RequestParam String titulo, Pageable pageable) {
        if (titulo == null || titulo.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.error("El parámetro 'titulo' es requerido"));
        String id = authHelper.usuarioAutenticado().getId();
        Page<Evento> page = serviceEvento.buscarPorOrganizadorYTitulo(id, titulo, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Resultados de búsqueda", toPagedDTO(page)));
    }

    @GetMapping("/admin/buscar")
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> buscarAdmin(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String categoriaId,
            @RequestParam(required = false) String estadoId,
            Pageable pageable) {
        Page<Evento> page = serviceEvento.buscarAdmin(titulo, categoriaId, estadoId, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Resultados de búsqueda", toPagedDTO(page)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<EventoDTO>> crear(@RequestBody Evento evento) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Evento creado", toDTO(serviceEvento.crearEvento(evento))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<EventoDTO>> actualizar(@PathVariable String id,
                                                              @RequestBody Evento evento) {
        return ResponseEntity.ok(ApiResponse.ok("Evento actualizado", toDTO(serviceEvento.actualizarEvento(id, evento))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable String id) {
        serviceEvento.eliminarEvento(id);
        return ResponseEntity.ok(ApiResponse.ok("Evento eliminado"));
    }

    // --- helpers de mapeo ---

    private EventoDTO toDTO(Evento e) {
        EventoDTO dto = new EventoDTO();
        dto.setId(e.getId());
        dto.setTitulo(e.getTitulo());
        dto.setDescripcion(e.getDescripcion());
        dto.setLugar(e.getLugar());
        dto.setFoto(e.getFoto() != null && !e.getFoto().isBlank() ? e.getFoto() : null);
        dto.setFecha(e.getFecha());
        dto.setHora(e.getHora());
        dto.setLocalidades(e.getLocalidades());
        if (e.getCategoria() != null)
            dto.setCategoria(new EventoDTO.Categoria(e.getCategoria().getId(), e.getCategoria().getNombre()));
        if (e.getEstado() != null)
            dto.setEstado(new EventoDTO.Estado(e.getEstado().getId(), e.getEstado().getNombre()));
        if (e.getOrganizador() != null)
            dto.setOrganizador(new EventoDTO.Organizador(
                    e.getOrganizador().getId(),
                    e.getOrganizador().getNombre(),
                    e.getOrganizador().getEsVerificado()));
        return dto;
    }

    private PagedResponse<EventoDTO> toPagedDTO(Page<Evento> page) {
        return new PagedResponse<>(
                page.getContent().stream().map(this::toDTO).toList(),
                page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}