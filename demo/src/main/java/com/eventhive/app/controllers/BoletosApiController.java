package com.eventhive.app.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.response.BoletosCompraDTO;
import com.eventhive.app.service.ServiceBoletos;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boletos")
public class BoletosApiController {

    private final ServiceBoletos serviceBoletos;

    @GetMapping("/{compraId}")
    public ResponseEntity<ApiResponse<BoletosCompraDTO>> obtener(@PathVariable Long compraId) {
        return ResponseEntity.ok(ApiResponse.ok("boletos de Compra obtenida", serviceBoletos.obtenerBoletosPorCompra(compraId)));
    }

    @PostMapping("/checkin/{codigoQR}")
    public ResponseEntity<ApiResponse<Void>> checkIn(@PathVariable String codigoQR) {
        serviceBoletos.realizarCheckIn(codigoQR);
        return ResponseEntity.ok(ApiResponse.ok("¡¡Tiquete valido!! check-in realizado"));
    }
}
