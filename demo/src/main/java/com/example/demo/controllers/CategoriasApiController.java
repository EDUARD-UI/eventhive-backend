package com.example.demo.controllers;

import java.util.List;

import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.CategoriaDTO;
import com.example.demo.dto.CategoriaEventosDTO;
import com.example.demo.dto.PagedResponse;
import com.example.demo.model.Categoria;
import com.example.demo.service.ServiceCategoria;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categorias")
public class CategoriasApiController {

    private final ServiceCategoria serviceCategoria;

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
    public ResponseEntity<ApiResponse<Categoria>> obtenerPorId(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Categoría obtenida",
                serviceCategoria.obtenerCategoriaPorId(id)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> crear(
            @RequestParam String nombre,
            @RequestParam(required = false) MultipartFile foto) {
        try {
            serviceCategoria.crearCategoria(nombre, foto);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Categoría creada"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> actualizar(
            @PathVariable String id,
            @RequestParam String nombre,
            @RequestParam(required = false) MultipartFile foto) {
        try {
            serviceCategoria.actualizarCategoria(id, nombre, foto);
            return ResponseEntity.ok(ApiResponse.ok("Categoría actualizada"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable String id) {
        serviceCategoria.eliminarCategoria(id);
        return ResponseEntity.ok(ApiResponse.ok("Categoría eliminada"));
    }
}
