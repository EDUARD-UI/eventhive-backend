package com.eventhive.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.dto.BoletosCompraDTO;
import com.eventhive.app.dto.CompraResponseDTO;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Tiquete;
import com.eventhive.app.repository.TiqueteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceBoletos {

    private final ServiceCompra serviceCompra;
    private final TiqueteRepository tiqueteRepository;

    @Transactional(readOnly = true)
    public BoletosCompraDTO obtenerBoletosPorCompra(Long compraId) {
        CompraResponseDTO compra = serviceCompra.obtenerPorId(compraId);

        List<Tiquete> tiquetes = tiqueteRepository.findByCompraIdConDetalles(compraId);

        BoletosCompraDTO dto = new BoletosCompraDTO();
        dto.setId(compra.getId());
        dto.setFechaCompra(compra.getFechaCompra());
        dto.setTotal(compra.getTotal());
        dto.setMetodoPago(compra.getMetodoPago());
        dto.setTiqueteCompras(tiquetes.stream()
                .map(this::toBoletoDTO)
                .toList());

        return dto;
    }

    private BoletosCompraDTO.BoletoDTO toBoletoDTO(Tiquete tiquete) {
        BoletosCompraDTO.BoletoDTO boleto = new BoletosCompraDTO.BoletoDTO();
        boleto.setId(tiquete.getId());

        BoletosCompraDTO.TiqueteDTO tiqueteDTO = new BoletosCompraDTO.TiqueteDTO();
        tiqueteDTO.setId(tiquete.getId());
        tiqueteDTO.setCodigoQR(tiquete.getCodigoQR());

        Evento evento = tiquete.getEvento();
        if (evento != null) {
            BoletosCompraDTO.LocalidadDTO localidadDTO = new BoletosCompraDTO.LocalidadDTO();
            Long localidadId = tiquete.getLocalidad() != null ? tiquete.getLocalidad().getId() : null;
            localidadDTO.setId(localidadId);

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
    }
}
