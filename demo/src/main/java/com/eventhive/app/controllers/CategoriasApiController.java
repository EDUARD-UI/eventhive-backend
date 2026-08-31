package com.eventhive.app.controllers;

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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.request.CategoriaRequest;
import com.eventhive.app.dto.response.CategoriaDTO;
import com.eventhive.app.dto.response.CategoriaEventosDTO;
import com.eventhive.app.service.ServiceCategoria;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categorias")
public class CategoriasApiController {

    private final ServiceCategoria serviceCategoria;

    //CONSULTAS
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CategoriaDTO>>> listar(Pageable pageable) {
        Page<CategoriaDTO> page = serviceCategoria.obtenerTodasCategorias(pageable)
                .map(categoria -> serviceCategoria.toDTO(categoria));

        return ResponseEntity.ok(ApiResponse.ok("Categorías obtenidas",
                new PagedResponse<>(
                        page.getContent(),
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages()
                )));
    }

    @GetMapping("/nombres")
    public ResponseEntity<ApiResponse<List<CategoriaDTO>>> listarDTO() {
        return ResponseEntity.ok(ApiResponse.ok("Categorías obtenidas",
                serviceCategoria.obtenerCategoriaDTO()));
    }

    @GetMapping("/destacadas")
    public ResponseEntity<ApiResponse<List<CategoriaDTO>>> destacadas() {
        return ResponseEntity.ok(ApiResponse.ok("Categorías destacadas",
                serviceCategoria.obtenerTop4Categorias()));
    }

    @GetMapping("/con-eventos")
    public ResponseEntity<ApiResponse<List<CategoriaEventosDTO>>> conEventos() {
        return ResponseEntity.ok(ApiResponse.ok("Categorías con eventos",
                serviceCategoria.obtenerCategoriasConEventos()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoriaDTO>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Categoría obtenida",
                serviceCategoria.toDTO(serviceCategoria.obtenerCategoriaPorId(id))));
    }

    //OPERACIONES CRUD
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> crear(
            @RequestPart("datos") @Valid CategoriaRequest request,
            @RequestPart(value = "foto", required = false) MultipartFile foto) {

        String nombreNormalizado = request.getNombre().trim();
        serviceCategoria.crearCategoria(nombreNormalizado, foto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Categoría creada exitosamente"));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> actualizar(
            @PathVariable Long id,
            @RequestPart("datos") @Valid CategoriaRequest request,
            @RequestPart(value = "foto", required = false) MultipartFile foto) {

        serviceCategoria.actualizarCategoria(id, request.getNombre(), foto);
        return ResponseEntity.ok(ApiResponse.ok("Categoría actualizada exitosamente"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        serviceCategoria.eliminarCategoria(id);
        return ResponseEntity.ok(ApiResponse.ok("Categoría eliminada exitosamente"));
    }
}
