package com.eventhive.app.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.BoletosCompraDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.model.Compra;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Tiquete;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.TiqueteRepository;
import com.eventhive.app.service.ServiceCompra;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boletos")
public class BoletosApiController {

    private final ServiceCompra serviceCompra;
    private final AuthenticatedUserHelper authHelper;
    private final TiqueteRepository tiqueteRepository;

    @GetMapping("/{compraId}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<BoletosCompraDTO>> obtener(@PathVariable Long compraId) {
        Usuario usuario = authHelper.usuarioAutenticado();
        Compra compra   = serviceCompra.obtenerPorId(compraId);

        if (!compra.getCliente().getId().equals(usuario.getId())) {
            throw new BusinessException("No autorizado");
        }

        // ── Buscar tiquetes asociados a esta compra ──────────────────────────
        List<Tiquete> tiquetes = tiqueteRepository.findByCompraIdConDetalles(compraId);

        BoletosCompraDTO dto = new BoletosCompraDTO();
        dto.setId(compra.getId());
        dto.setFechaCompra(compra.getFechaCompra());
        dto.setTotal(compra.getTotal());
        dto.setMetodoPago(compra.getMetodoPago());

        // Mapear cada tiquete a BoletoDTO con su información de localidad y evento
        List<BoletosCompraDTO.BoletoDTO> boletos = tiquetes.stream().map(tiquete -> {
            BoletosCompraDTO.BoletoDTO boleto = new BoletosCompraDTO.BoletoDTO();
            boleto.setId(tiquete.getId());

                BoletosCompraDTO.TiqueteDTO tiqueteDTO = new BoletosCompraDTO.TiqueteDTO();
            tiqueteDTO.setId(tiquete.getId());
            tiqueteDTO.setCodigoQR(tiquete.getCodigoQR());

            // Poblar datos de localidad y evento desde el tiquete
            Evento evento = tiquete.getEvento();
                if (evento != null) {
                BoletosCompraDTO.LocalidadDTO localidadDTO = new BoletosCompraDTO.LocalidadDTO();
                Long localidadId = tiquete.getLocalidad() != null ? tiquete.getLocalidad().getId() : null;
                localidadDTO.setId(localidadId);

                // Buscar la localidad dentro del evento por su id almacenado en el tiquete
                if (evento.getLocalidades() != null && localidadId != null) {
                    evento.getLocalidades().stream()
                        .filter(l -> localidadId.equals(l.getId()))
                        .findFirst()
                        .ifPresent(l -> {
                            localidadDTO.setNombre(l.getNombre());
                            localidadDTO.setPrecio(l.getPrecio());
                        });
                }

                BoletosCompraDTO.EventoDTO eventoDTO = new BoletosCompraDTO.EventoDTO();
                eventoDTO.setId(evento.getId());
                eventoDTO.setTitulo(evento.getTitulo());
                eventoDTO.setFecha(evento.getFecha());
                eventoDTO.setHora(evento.getHora());
                eventoDTO.setLugar(evento.getLugar());

                localidadDTO.setEvento(eventoDTO);
                tiqueteDTO.setLocalidad(localidadDTO);
            }

            boleto.setTiquete(tiqueteDTO);
            return boleto;
        }).collect(Collectors.toList());

        dto.setTiqueteCompras(boletos);

        return ResponseEntity.ok(ApiResponse.ok("Compra obtenida", dto));
    }
}
