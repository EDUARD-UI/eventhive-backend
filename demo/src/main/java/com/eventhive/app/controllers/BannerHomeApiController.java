package com.eventhive.app.controllers;

import java.util.List;

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
import com.eventhive.app.dto.response.BannerHomeDTO;
import com.eventhive.app.service.ServiceBannerHome;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/banners-home")
public class BannerHomeApiController {

    private final ServiceBannerHome serviceBannerHome;

    //CONSULTAS
    @GetMapping
    public ResponseEntity<ApiResponse<List<BannerHomeDTO>>> obtenerBannersPublicos() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Banners del Home obtenidos", serviceBannerHome.obtenerBannersPublicos()));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','MARKETING')")
    public ResponseEntity<ApiResponse<List<BannerHomeDTO>>> obtenerEspaciosAdministrativos() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Espacios de banners obtenidos", serviceBannerHome.obtenerEspaciosAdministrativos()));
    }

    //CREACIÓN
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','MARKETING')")
    public ResponseEntity<ApiResponse<BannerHomeDTO>> crear(
            @RequestParam int posicion,
            @RequestParam String titulo,
            @RequestParam String textoBoton,
            @RequestParam String enlaceUrl,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Banner creado",
                serviceBannerHome.crear(posicion, titulo, textoBoton, enlaceUrl, imagen)));
    }

    @PostMapping(value = "/{posicion}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','MARKETING')")
    public ResponseEntity<ApiResponse<BannerHomeDTO>> crearConPosicion(
            @PathVariable int posicion,
            @RequestParam String titulo,
            @RequestParam String textoBoton,
            @RequestParam String enlaceUrl,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Banner creado",
                serviceBannerHome.crear(posicion, titulo, textoBoton, enlaceUrl, imagen)));
    }

    //MODIFICACIONES
    @PutMapping(value = "/{posicion}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','MARKETING')")
    public ResponseEntity<ApiResponse<BannerHomeDTO>> actualizar(
            @PathVariable int posicion,
            @RequestParam String titulo,
            @RequestParam String textoBoton,
            @RequestParam String enlaceUrl,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Banner actualizado",
                serviceBannerHome.actualizar(posicion, titulo, textoBoton, enlaceUrl, imagen)));
    }

    //ELIMINACIÓN
    @DeleteMapping("/{posicion}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','MARKETING')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable int posicion) {
        serviceBannerHome.eliminar(posicion);
        return ResponseEntity.ok(ApiResponse.ok("Banner eliminado exitosamente"));
    }
}