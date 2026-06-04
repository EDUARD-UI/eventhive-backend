package com.example.demo.controllers;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.PagedResponse;
import com.example.demo.model.Compra;
import com.example.demo.model.ItemCompra;
import com.example.demo.service.ServiceCompra;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class CompraApiController {

    private final ServiceCompra compraService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<Compra>>> listar(Pageable pageable) {
        Page<Compra> page = compraService.listarMisCompras(pageable);
        PagedResponse<Compra> response = new PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
        return ResponseEntity.ok(ApiResponse.ok("Compras obtenidas", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Compra>> obtener(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Compra obtenida", 
            compraService.obtenerPorId(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Compra>> crear(@RequestBody List<ItemCompra> items) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Compra realizada",
            compraService.realizarCompra(items)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> cancelar(@PathVariable String id) {
        compraService.cancelarCompra(id);
        return ResponseEntity.ok(ApiResponse.ok("Compra cancelada"));
    }
}
