package com.eventhive.app.service;

import com.eventhive.app.dto.response.CompraResponseDTO;
import com.eventhive.app.dto.response.ItemCompraDTO;
import com.eventhive.app.dto.request.CompraRequestDTO;
import com.eventhive.app.enums.EstadoCompra;
import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.*;
import com.eventhive.app.repository.CompraRepository;
import com.eventhive.app.repository.LocalidadRepository;
import com.eventhive.app.repository.PromocionRepository;
import com.eventhive.app.repository.TiqueteRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceCompra {
    
    private final CompraRepository compraRepository;
    private final LocalidadRepository localidadRepository;
    private final TiqueteRepository tiqueteRepository;
    private final PromocionRepository promocionRepository;
    private final AuthenticatedUserHelper authHelper;
    private final ServiceCorreo serviceCorreo;

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<CompraResponseDTO> listarMisCompras(Pageable pageable) {
        Usuario usuario = authHelper.usuarioAutenticado();
        return compraRepository
                .findByClienteIdConItems(usuario.getId(), pageable)
                .map(this::compraToDTO);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public CompraResponseDTO obtenerPorId(Long id) {
        Usuario usuario = authHelper.usuarioAutenticado();
        Compra compra = buscarCompra(id);
        validarOwnership(compra, usuario);
        return compraToDTO(compra);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public CompraResponseDTO realizarCompra(CompraRequestDTO request) {
        validarRequest(request);

        Usuario usuario = authHelper.usuarioAutenticado();

        Optional<Compra> existente = compraRepository
                .findByClienteIdAndIdempotencyKey(usuario.getId(), request.getIdempotencyKey());
        if (existente.isPresent()) {
            return compraToDTO(existente.get());
        }

        List<ItemCompra> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CompraRequestDTO.ItemRequest itemReq : request.getItems()) {
            Localidad localidad = buscarLocalidad(itemReq.getLocalidadId());
            validarEventoPublicado(localidad);
            descontarDisponibles(localidad, itemReq.getCantidad());

            BigDecimal precio = calcularPrecioConPromocion(localidad);
            items.add(buildItem(localidad, itemReq.getCantidad(), precio));
            total = total.add(precio.multiply(BigDecimal.valueOf(itemReq.getCantidad())));
        }

        try {
            Compra guardada = guardarCompra(usuario, items, total, request.getIdempotencyKey());
            generarTiquetes(items, guardada);
            serviceCorreo.enviarConfirmacionCompra(guardada);
            return compraToDTO(guardada);
        } catch (DataIntegrityViolationException e) {
            return compraRepository.findByClienteIdAndIdempotencyKey(usuario.getId(), request.getIdempotencyKey())
                    .map(this::compraToDTO)
                    .orElseThrow(() -> e);
        }
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void cancelarCompra(Long id) {
        Usuario usuario = authHelper.usuarioAutenticado();
        Compra compra = buscarCompra(id);
        validarOwnership(compra, usuario);
        validarCancelable(compra);
        compra.setEstado(EstadoCompra.CANCELADA);
        compra.getItems().forEach(item
                -> localidadRepository.incrementarDisponibles(
                item.getLocalidad().getId(), item.getCantidad()));

        compraRepository.deleteById(id);
        tiqueteRepository.deleteByCompraId(id);
    }

    // validaciones de negocios
    private void validarEventoPublicado(Localidad localidad) {
        if (localidad.getEvento().getEstado() != EstadoEvento.PUBLICADO) {
            throw new BusinessException(
                    "No se pueden vender boletos para un evento que no está publicado");
        }
    }

    private void validarCancelable(Compra compra) {
        if (compra.getEstado() == EstadoCompra.CANCELADA) {
            throw new BusinessException("La compra ya se encuentra cancelada");
        }
    }

    private void validarRequest(CompraRequestDTO request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Debe incluir al menos un ítem en la compra");
        }
    }

    //se deja en error 404 para no dar pista de que existen
    private void validarOwnership(Compra compra, Usuario usuario) {
        if (!compra.getCliente().getId().equals(usuario.getId())) {
            throw new ResourceNotFoundException("Compra no encontrada con id: ");
        }
    }

    private Compra buscarCompra(Long id) {
        return compraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada con id: " + id));
    }

    private Localidad buscarLocalidad(Long localidadId) {
        return localidadRepository.findByIdConEvento(localidadId)
                .orElseThrow(() -> new ResourceNotFoundException("Localidad no encontrada: " + localidadId));
    }

    private void descontarDisponibles(Localidad localidad, Integer cantidad) {
        int filasAfectadas = localidadRepository.decrementarDisponibles(localidad.getId(), cantidad);
        if (filasAfectadas == 0) {
            throw new BusinessException(
                    "No hay suficientes entradas en '" + localidad.getNombre()
                            + "'. Disponibles: " + localidad.getDisponibles()
                            + ", solicitados: " + cantidad);
        }
    }

    private BigDecimal calcularPrecioConPromocion(Localidad localidad) {
        return promocionRepository
                .findVigenteByEventoId(localidad.getEvento().getId(), LocalDate.now())
                .map(promo -> localidad.getPrecio()
                        .multiply(BigDecimal.valueOf(100)
                                .subtract(BigDecimal.valueOf(promo.getDescuento())))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .orElse(localidad.getPrecio());
    }

    private ItemCompra buildItem(Localidad localidad, Integer cantidad, BigDecimal precio) {
        ItemCompra item = new ItemCompra();
        item.setLocalidad(localidad);
        item.setEvento(localidad.getEvento());
        item.setCantidad(cantidad);
        item.setPrecioUnitario(precio);
        return item;
    }

    private Compra guardarCompra(Usuario usuario, List<ItemCompra> items, BigDecimal total, String idempotencyKey) {
        Compra compra = new Compra();
        compra.setIdempotencyKey(idempotencyKey);
        compra.setFechaCompra(LocalDateTime.now());
        compra.setTotal(total);
        compra.setMetodoPago("TARJETA");
        compra.setCliente(usuario);
        compra.setEstado(EstadoCompra.CONFIRMADA);
        items.forEach(i -> i.setCompra(compra));
        compra.setItems(items);
        return compraRepository.save(compra);
    }

    private void generarTiquetes(List<ItemCompra> items, Compra compra) {
        List<Tiquete> tiquetes = new ArrayList<>();

        for (ItemCompra item : items) {
            for (int i = 0; i < item.getCantidad(); i++) {
                Tiquete t = new Tiquete();
                t.setCodigoQR(UUID.randomUUID().toString());
                t.setLocalidad(item.getLocalidad());
                t.setEvento(item.getEvento());
                t.setCompra(compra);
                tiquetes.add(t);
            }
        }

        tiqueteRepository.saveAll(tiquetes);
    }

    // MAPEO
    private CompraResponseDTO compraToDTO(Compra compra) {
        return CompraResponseDTO.builder()
                .id(compra.getId())
                .fechaCompra(compra.getFechaCompra())
                .total(compra.getTotal())
                .metodoPago(compra.getMetodoPago())
                .items(itemsToDTO(compra.getItems()))
                .build();
    }

    private List<ItemCompraDTO> itemsToDTO(List<ItemCompra> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream().map(this::itemToDTO).toList();
    }

    private ItemCompraDTO itemToDTO(ItemCompra item) {
        return ItemCompraDTO.builder()
                .localidadId(item.getLocalidad().getId())
                .localidadNombre(item.getLocalidad().getNombre())
                .eventoNombre(item.getEvento().getTitulo())
                .cantidad(item.getCantidad())
                .precioUnitario(item.getPrecioUnitario())
                .subtotal(item.getPrecioUnitario()
                        .multiply(BigDecimal.valueOf(item.getCantidad())))
                .build();
    }
}
