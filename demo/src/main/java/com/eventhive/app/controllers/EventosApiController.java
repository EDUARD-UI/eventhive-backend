package com.eventhive.app.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.request.EventoRequest;
import com.eventhive.app.dto.response.EventoDTO;
import com.eventhive.app.dto.response.EventoDestacadoDTO;
import com.eventhive.app.dto.response.EventoMapaDTO;
import com.eventhive.app.service.ServiceEvento;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
public class EventosApiController {

    private final ServiceEvento serviceEvento;
    private final AuthenticatedUserHelper authHelper;

    //CONSULTAS
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> listar(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Pageable pageable) {

        var page = serviceEvento.listarPublicos(categoriaId, fecha, pageable);

        return ResponseEntity.ok(ApiResponse.ok("Eventos obtenidos", serviceEvento.toPagedDTO(page)));
    }

    // Eventos destacados: solo promocionados que además están PUBLICADOS.
    @GetMapping("/destacados")
        public ResponseEntity<ApiResponse<PagedResponse<EventoDestacadoDTO>>> eventosDestacados(
            @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Eventos destacados obtenidos",
                serviceEvento.toPagedDestacadosDTO(serviceEvento.listarDestacados(pageable))
        ));
    }

    //eventos cercanos a la fecha actual
    @GetMapping("/proximos")
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> eventosProximos(
            @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Próximos eventos obtenidos",
                serviceEvento.toPagedDTO(serviceEvento.listarEventosProximos(pageable))
        ));
    }

    //solo devuelve eventos de estado.PUBLICADO
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventoDTO>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Evento obtenido",
                serviceEvento.toDetalleDTO(serviceEvento.obtenerEventoPublicoPorId(id))));
    }

    @GetMapping("/organizador/{id}")
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
    public ResponseEntity<ApiResponse<EventoDTO>> obtenerDeMiOrganizacion(@PathVariable Long id) {
        Long organizacionId = authHelper.usuarioAutenticado().getOrganizacion().getId();
        return ResponseEntity.ok(ApiResponse.ok("Evento obtenido",
                serviceEvento.toDetalleDTO(serviceEvento.obtenerEventoDeOrganizacionPorId(organizacionId, id))));
    }

    @GetMapping("/organizador")
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> eventosPorOrganizacion(Pageable pageable) {
        Long organizacionId = authHelper.usuarioAutenticado().getOrganizacion().getId();
        return ResponseEntity.ok(ApiResponse.ok("Eventos obtenidos",
                serviceEvento.toPagedDTO(serviceEvento.listarPorOrganizacion(organizacionId, pageable))));
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('MODERADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<EventoDTO>> obtenerAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Evento obtenido",
                serviceEvento.toDetalleDTO(serviceEvento.obtenerEventoAdministrativo(id))));
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
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> buscar(
            @RequestParam(name = "nombre", required = false) String nombre,
            @RequestParam(name = "titulo", required = false) String titulo,
            Pageable pageable) {

        String termino = (nombre != null && !nombre.isBlank()) ? nombre : titulo;
        if (termino == null || termino.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El parámetro 'nombre' es requerido"));
        }

        var page = serviceEvento.buscarEventos(termino, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Resultados de búsqueda",
                new PagedResponse<>(page.getContent(), page.getNumber(),
                        page.getSize(), page.getTotalElements(), page.getTotalPages())));
    }

    @GetMapping("/organizador/buscar")
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
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
    @PreAuthorize("hasRole('MODERADOR') or hasRole('ADMINISTRADOR')")
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
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
    public ResponseEntity<ApiResponse<EventoDTO>> crear(
            @RequestPart("datos") @Valid EventoRequest request,
            @RequestPart(value = "foto", required = false) MultipartFile foto){

        var evento = serviceEvento.crearEvento(request, foto);
                var organizacion = evento.getOrganizacion();
                String mensaje;
                if (organizacion.getEstado() == com.eventhive.app.enums.EstadoOrganizacion.PENDIENTE_REVISION) {
                        mensaje = organizacion.getUrlRut() == null || organizacion.getUrlRut().isBlank()
                                        ? "Organización PENDIENTE_REVISION. Evento creado como BORRADOR; adjunta el RUT para solicitar verificación."
                                        : "Organización PENDIENTE_REVISION. Evento creado como BORRADOR; podrá enviarse a moderación cuando la organización sea aprobada.";
                } else {
                        mensaje = "Organización APROBADA. Evento creado como BORRADOR; envíalo a moderación cuando esté completo.";
                }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(mensaje, serviceEvento.toDetalleDTO(evento)));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
    public ResponseEntity<ApiResponse<EventoDTO>> actualizar(
            @PathVariable Long id,
            @RequestPart("datos") @Valid EventoRequest request,
            @RequestPart(value = "foto", required = false) MultipartFile foto) {

        return ResponseEntity.ok(ApiResponse.ok("Evento actualizado",
                serviceEvento.toDetalleDTO(serviceEvento.actualizarEvento(id, request, foto))));
    }

        @PatchMapping("/{id}/enviar-revision")
        @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
        public ResponseEntity<ApiResponse<Void>> enviarRevision(@PathVariable Long id) {
                serviceEvento.enviarRevision(id);
                return ResponseEntity.ok(ApiResponse.ok("Evento enviado a revisión"));
        }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
    public ResponseEntity<ApiResponse<Void>> cancelarEvento(@PathVariable Long id){
        serviceEvento.cancelarEvento(id);
        return ResponseEntity.ok(ApiResponse.ok("evento cancelado"));
    }

    @PatchMapping("/{id}/retirar")
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
    public ResponseEntity<ApiResponse<Void>> retirarEvento(@PathVariable Long id) {
        serviceEvento.retirarEvento(id);
        return ResponseEntity.ok(ApiResponse.ok("Evento retirado a borrador"));
    }

    @PatchMapping("/{id}/reabrir")
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
    public ResponseEntity<ApiResponse<Void>> reabrirEvento(@PathVariable Long id) {
        serviceEvento.reabrirEvento(id);
        return ResponseEntity.ok(ApiResponse.ok("Evento reabierto a borrador"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        serviceEvento.eliminarEvento(id);
        return ResponseEntity.ok(ApiResponse.ok("Evento eliminado"));
    }
}
