package com.eventhive.app.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.request.ValidarCheckInRequest;
import com.eventhive.app.dto.response.BoletosCompraDTO;
import com.eventhive.app.service.ServiceBoletos;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boletos")
public class BoletosApiController {

    private final ServiceBoletos serviceBoletos;

    @GetMapping("/{compraId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BoletosCompraDTO>> obtener(@PathVariable Long compraId) {
        return ResponseEntity.ok(ApiResponse.ok("boletos de Compra obtenida", serviceBoletos.obtenerBoletosPorCompra(compraId)));
    }

    @GetMapping(value = {"/validar", "/validar/**"})
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
    public ResponseEntity<ApiResponse<BoletosCompraDTO.BoletoDTO>> validar(
            @RequestParam(required = false) String codigoQR,
            @RequestParam(required = false) String codigo,
            HttpServletRequest request) {
        String finalQr = resolverCodigo(codigoQR, codigo, request, "/api/boletos/validar");
        return ResponseEntity.ok(ApiResponse.ok("Boleto válido", serviceBoletos.validarBoleto(finalQr)));
    }

    @PostMapping(value = {"/validar", "/validar/**"})
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
    public ResponseEntity<ApiResponse<BoletosCompraDTO.BoletoDTO>> validarPost(
            @RequestBody(required = false) ValidarCheckInRequest body,
            @RequestParam(required = false) String codigoQR,
            HttpServletRequest request) {
        String finalQr = body != null && body.getCodigoQR() != null && !body.getCodigoQR().isBlank()
                ? body.getCodigoQR()
                : resolverCodigo(codigoQR, body != null ? body.getCodigo() : null, request, "/api/boletos/validar");
        return ResponseEntity.ok(ApiResponse.ok("Boleto válido", serviceBoletos.validarBoleto(finalQr)));
    }

    @PostMapping(value = {"/checkin", "/checkin/**"})
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
    public ResponseEntity<ApiResponse<Void>> checkIn(
            @RequestBody(required = false) ValidarCheckInRequest body,
            @RequestParam(required = false) String codigoQR,
            @RequestParam(required = false) String codigo,
            HttpServletRequest request) {
        String finalQr = body != null && body.getCodigoQR() != null && !body.getCodigoQR().isBlank()
                ? body.getCodigoQR()
                : resolverCodigo(codigoQR, body != null ? body.getCodigo() : codigo, request, "/api/boletos/checkin");
        serviceBoletos.realizarCheckIn(finalQr);
        return ResponseEntity.ok(ApiResponse.ok("¡¡Tiquete valido!! check-in realizado"));
    }

    private String resolverCodigo(String codigoQR, String codigo, HttpServletRequest request, String prefix) {
        if (codigoQR != null && !codigoQR.isBlank()) {
            return codigoQR.trim();
        }
        if (codigo != null && !codigo.isBlank()) {
            return codigo.trim();
        }
        if (request != null) {
            String uri = request.getRequestURI();
            int idx = uri.indexOf(prefix);
            if (idx != -1) {
                String sub = uri.substring(idx + prefix.length());
                if (sub.startsWith("/")) {
                    sub = sub.substring(1);
                }
                if (!sub.isBlank()) {
                    if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
                        sub = sub + "?" + request.getQueryString();
                    }
                    return sub.trim();
                }
            }
            if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
                return request.getQueryString().trim();
            }
        }
        return "";
    }
}
