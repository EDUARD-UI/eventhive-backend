package com.eventhive.app.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.dto.BoletosCompraDTO;
import com.eventhive.app.dto.CompraResponseDTO;
import com.eventhive.app.enums.EstadoCompra;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Localidad;
import com.eventhive.app.model.Tiquete;
import com.eventhive.app.repository.TiqueteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceBoletos {

    private final ServiceCompra serviceCompra;
    private final TiqueteRepository tiqueteRepository;

    //validar el ingreso de un tiquete por codigo qr
    @Transactional
    @PreAuthorize("hasRole('ORGANIZACION') or hasRole('ADMINISTRADOR')")
    public void realizarCheckIn(String codigoQR) {
        Tiquete tiquete = tiqueteRepository.findByCodigoQR(codigoQR)
                .orElseThrow(() -> new ResourceNotFoundException("Tiquete no encontrado o inválido"));

        if (tiquete.getCompra().getEstado() == EstadoCompra.CANCELADA) {
            throw new BusinessException("El tiquete pertenece a una compra cancelada");
        }
        if (tiquete.isUsado()) {
            throw new BusinessException("El tiquete ya fue utilizado");
        }
        tiquete.setUsado(true);
        tiqueteRepository.save(tiquete);
    }

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

        Localidad localidad = tiquete.getLocalidad();
        if (localidad != null) {
            BoletosCompraDTO.LocalidadDTO localidadDTO = new BoletosCompraDTO.LocalidadDTO();
            localidadDTO.setId(localidad.getId());
            localidadDTO.setNombre(localidad.getNombre());
            localidadDTO.setPrecio(localidad.getPrecio());

            Evento evento = tiquete.getEvento();
            if (evento != null) {
                BoletosCompraDTO.EventoDTO eventoDTO = new BoletosCompraDTO.EventoDTO();
                eventoDTO.setId(evento.getId());
                eventoDTO.setTitulo(evento.getTitulo());
                eventoDTO.setFecha(evento.getFecha());
                eventoDTO.setHora(evento.getHora());
                eventoDTO.setLugar(evento.getLugar());
                localidadDTO.setEvento(eventoDTO);
            }

            tiqueteDTO.setLocalidad(localidadDTO);
        }

        boleto.setTiquete(tiqueteDTO);
        return boleto;
    }
}
