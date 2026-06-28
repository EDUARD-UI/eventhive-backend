package com.eventhive.app.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.dto.CompraResponseDTO;
import com.eventhive.app.dto.ItemCompraDTO;
import com.eventhive.app.dto.request.CompraRequestDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Compra;
import com.eventhive.app.model.ItemCompra;
import com.eventhive.app.model.Localidad;
import com.eventhive.app.model.Tiquete;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.CompraRepository;
import com.eventhive.app.repository.LocalidadRepository;
import com.eventhive.app.repository.PromocionRepository;
import com.eventhive.app.repository.TiqueteRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceCompra {

    private final CompraRepository       compraRepository;
    private final LocalidadRepository    localidadRepository;
    private final TiqueteRepository      tiqueteRepository;
    private final PromocionRepository    promocionRepository;
    private final AuthenticatedUserHelper authHelper;
    private final ServiceUsuario         serviceUsuario;
    private final ServiceNotification    serviceNotification;

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
        Compra compra   = buscarCompra(id);
        validarOwnership(compra, usuario);
        return compraToDTO(compra);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public CompraResponseDTO realizarCompra(CompraRequestDTO request) {
        validarRequest(request);

        Usuario usuario           = authHelper.usuarioAutenticado();
        List<ItemCompra> items    = new ArrayList<>();
        BigDecimal total          = BigDecimal.ZERO;

        for (CompraRequestDTO.ItemRequest itemReq : request.getItems()) {
            Localidad localidad = buscarLocalidad(itemReq.getLocalidadId());
            descontarDisponibles(localidad, itemReq.getCantidad());

            BigDecimal precio = calcularPrecioConPromocion(localidad);
            items.add(buildItem(localidad, itemReq.getCantidad(), precio));
            total = total.add(precio.multiply(BigDecimal.valueOf(itemReq.getCantidad())));
        }

        Compra guardada = guardarCompra(usuario, items, total);
        generarTiquetes(items, guardada);
        actualizarFidelizacion(usuario);

        return compraToDTO(guardada);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void cancelarCompra(Long id) {
        Usuario usuario = authHelper.usuarioAutenticado();
        Compra compra   = buscarCompra(id);
        validarOwnership(compra, usuario);

        compra.getItems().forEach(item ->
                localidadRepository.incrementarDisponibles(
                        item.getLocalidad().getId(), item.getCantidad()));

        compraRepository.deleteById(id);
    }
    
    private void validarRequest(CompraRequestDTO request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Debe incluir al menos un ítem en la compra");
        }
    }

    private void validarOwnership(Compra compra, Usuario usuario) {
        if (!compra.getCliente().getId().equals(usuario.getId())) {
            throw new BusinessException("No autorizado para operar sobre esta compra");
        }
    }

    // HELPERS — NEGOCIO
    private Compra buscarCompra(Long id) {
        return compraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada con id: " + id));
    }

    private Localidad buscarLocalidad(Long localidadId) {
        return localidadRepository.findByIdConEvento(localidadId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Localidad no encontrada: " + localidadId));
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

    private Compra guardarCompra(Usuario usuario, List<ItemCompra> items, BigDecimal total) {
        Compra compra = new Compra();
        compra.setFechaCompra(LocalDateTime.now());
        compra.setTotal(total);
        compra.setMetodoPago("TARJETA");
        compra.setCliente(usuario);
        items.forEach(i -> i.setCompra(compra));
        compra.setItems(items);
        return compraRepository.save(compra);
    }

    private void generarTiquetes(List<ItemCompra> items, Compra compra) {
        items.forEach(item -> {
            for (int i = 0; i < item.getCantidad(); i++) {
                Tiquete t = new Tiquete();
                t.setCodigoQR(UUID.randomUUID().toString());
                t.setLocalidad(item.getLocalidad());
                t.setEvento(item.getEvento());
                t.setCompra(compra);
                tiqueteRepository.save(t);
            }
        });
    }

    private void actualizarFidelizacion(Usuario usuario) {
        try {
            usuario.setCantidadCompras(usuario.getCantidadCompras() + 1);
            boolean subioNivel = serviceUsuario.actualizarNivelSiCambia(usuario);
            if (subioNivel) serviceNotification.notificarNuevoNivel(usuario);
        } catch (Exception ex) {
            log.warn("Fallo en fidelización para usuario {}: {}", usuario.getId(), ex.getMessage());
        }
    }

    // HELPERS — MAPEO Entidad → DTO
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
        if (items == null) return List.of();
        return items.stream()
                .map(this::itemToDTO)
                .toList();
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