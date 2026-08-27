package com.eventhive.app.service;

import com.eventhive.app.dto.response.BoletosCompraDTO;
import com.eventhive.app.dto.response.CompraResponseDTO;
import com.eventhive.app.enums.EstadoCompra;
import com.eventhive.app.enums.PermisoEvento;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Localidad;
import com.eventhive.app.model.Tiquete;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.TiqueteRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceBoletos {

    private final ServiceCompra serviceCompra;
    private final TiqueteRepository tiqueteRepository;
    private final AuthenticatedUserHelper authHelper;

    //validar el ingreso de un tiquete por codigo qr
    @Transactional
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
    public void realizarCheckIn(String codigoQR) {
        Tiquete tiquete = tiqueteRepository.findByCodigoQR(codigoQR)
                .orElseThrow(() -> new ResourceNotFoundException("Tiquete no encontrado o inválido"));

        validarPermisoCheckIn(tiquete);

        if (tiquete.getCompra().getEstado() == EstadoCompra.CANCELADA) {
            throw new BusinessException("El tiquete pertenece a una compra cancelada");
        }

        // bug #15: UPDATE ... WHERE usado = false, en vez de leer y luego escribir.
        // Así solo una petición concurrente puede consumir el tiquete.
        int filasActualizadas = tiqueteRepository.marcarComoUsadoSiNoUsado(codigoQR);
        if (filasActualizadas == 0) {
            throw new BusinessException("El tiquete ya fue utilizado");
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('CLIENTE')")
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

    //validar IDOR
    private void validarPermisoCheckIn(Tiquete tiquete) {
        Usuario operador = authHelper.usuarioAutenticado();

        Long organizacionID = tiquete.getEvento().getOrganizacion().getId();
        if (!operador.getOrganizacion().getId().equals(organizacionID)) {
            throw new ResourceNotFoundException("Ups, No esta autorizado para operar este tiquete");
        }

        boolean esRepresentante = operador.getOrganizacion().getRepresentante().getId().equals(operador.getId());
        if (!esRepresentante && !operador.getPermisosEvento().contains(PermisoEvento.CHECK_IN)) {
            throw new BusinessException("No tienes permiso de check-in en esta organización");
        }
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
