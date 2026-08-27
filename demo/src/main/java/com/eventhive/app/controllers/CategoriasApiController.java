package com.eventhive.app.controllers;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.request.CategoriaRequest;
import com.eventhive.app.dto.response.CategoriaDTO;
import com.eventhive.app.dto.response.CategoriaEventosDTO;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.model.Categoria;
import com.eventhive.app.service.ServiceCategoria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categorias")
public class CategoriasApiController {

    private final ServiceCategoria serviceCategoria;

    //CONSULTAS
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<Categoria>>> obtener(Pageable pageable) {
        Page<Categoria> page = serviceCategoria.obtenerTodasCategorias(pageable);

        page.getContent().forEach(cat -> {
            if (cat.getFoto() != null && cat.getFoto().trim().isEmpty()) {
                cat.setFoto(null);
            }
        });

        PagedResponse<Categoria> response = new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );

        return ResponseEntity.ok(ApiResponse.ok("Categorías obtenidas", response));
    }

    @GetMapping("/nombres")
    public ResponseEntity<ApiResponse<List<CategoriaDTO>>> listar() {
        List<CategoriaDTO> categorias = serviceCategoria.obtenerCategoriaDTO();
        categorias.forEach(cat -> {
            if (cat.getNombre() != null && cat.getNombre().isEmpty()) {
                cat.setId(null);
            }
        });
        return ResponseEntity.ok(ApiResponse.ok("Categorías obtenidas", categorias));
    }

    @GetMapping("/destacadas")
    public ResponseEntity<ApiResponse<List<Categoria>>> destacadas() {
        return ResponseEntity.ok(ApiResponse.ok("Categorías destacadas",
                serviceCategoria.obtenerTop4Categorias()));
    }

    @GetMapping("/con-eventos")
    public ResponseEntity<ApiResponse<List<CategoriaEventosDTO>>> conEventos() {
        return ResponseEntity.ok(ApiResponse.ok("Categorías con eventos",
                serviceCategoria.obtenerCategoriasConEventos()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Categoria>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Categoría obtenida",
                serviceCategoria.obtenerCategoriaPorId(id)));
    }

    //OPERACIONES CRUD
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> crear(
            @RequestBody CategoriaRequest request,
            @RequestParam(required = false) MultipartFile foto) {
        try {
            serviceCategoria.crearCategoria(request.getNombre(), foto);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Categoría creada exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> actualizar(
            @PathVariable Long id,
            @RequestParam String nombre,
            @RequestParam(required = false) MultipartFile foto) {
        try {
            serviceCategoria.actualizarCategoria(id, nombre, foto);
            return ResponseEntity.ok(ApiResponse.ok("Categoría actualizada exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        serviceCategoria.eliminarCategoria(id);
        return ResponseEntity.ok(ApiResponse.ok("Categoría eliminada exitosamente"));
    }
}
