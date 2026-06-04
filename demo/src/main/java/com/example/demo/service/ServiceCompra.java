package com.example.demo.service;

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

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Compra;
import com.example.demo.model.ItemCompra;
import com.example.demo.model.Localidad;
import com.example.demo.model.Tiquete;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CompraRepository;
import com.example.demo.repository.LocalidadRepository;
import com.example.demo.repository.PromocionRepository;
import com.example.demo.repository.TiqueteRepository;
import com.example.demo.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceCompra {

    private final CompraRepository    compraRepository;
    private final LocalidadRepository localidadRepository;
    private final TiqueteRepository   tiqueteRepository;
    private final PromocionRepository promocionRepository;
    private final AuthenticatedUserHelper authHelper;

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Compra> listarMisCompras(Pageable pageable) {
        Usuario u = authHelper.usuarioAutenticado();
        return compraRepository.findByClienteIdConItems(u.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Compra obtenerPorId(String id) {
        return compraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada con id: " + id));
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public Compra realizarCompra(List<ItemCompra> items) {
        if (items == null || items.isEmpty())
            throw new BusinessException("Debe incluir al menos un ítem en la compra");

        Usuario usuario = authHelper.usuarioAutenticado();
        BigDecimal total = BigDecimal.ZERO;
        List<ItemCompra> itemsValidados = new ArrayList<>();

        for (ItemCompra item : items) {
            if (item.getLocalidad() == null || item.getLocalidad().getId() == null)
                throw new BusinessException("Cada ítem debe tener una localidad válida");

            if (item.getCantidad() == null || item.getCantidad() <= 0)
                throw new BusinessException("La cantidad debe ser mayor a 0");

            Localidad localidad = localidadRepository.findByIdConEvento(item.getLocalidad().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Localidad no encontrada: " + item.getLocalidad().getId()));

            // Decremento atómico — devuelve 0 si no hay suficientes disponibles
            int filasAfectadas = localidadRepository.decrementarDisponibles(
                    localidad.getId(), item.getCantidad());

            if (filasAfectadas == 0)
                throw new BusinessException(
                        "No hay suficientes entradas disponibles en '" + localidad.getNombre()
                        + "'. Disponibles: " + localidad.getDisponibles()
                        + ", solicitados: " + item.getCantidad());

            BigDecimal precio = calcularPrecioConPromocion(localidad);
            item.setLocalidad(localidad);
            item.setEvento(localidad.getEvento());
            item.setPrecioUnitario(precio);
            total = total.add(precio.multiply(BigDecimal.valueOf(item.getCantidad())));
            itemsValidados.add(item);
        }

        Compra compra = new Compra();
        compra.setFechaCompra(LocalDateTime.now());
        compra.setTotal(total);
        compra.setMetodoPago("TARJETA");
        compra.setCliente(usuario);
        itemsValidados.forEach(i -> i.setCompra(compra));
        compra.setItems(itemsValidados);

        Compra guardada = compraRepository.save(compra);
        generarTiquetes(itemsValidados, guardada);
        return guardada;
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void cancelarCompra(String id) {
        Compra compra = obtenerPorId(id);
        Usuario u = authHelper.usuarioAutenticado();

        if (!compra.getCliente().getId().equals(u.getId()))
            throw new BusinessException("No autorizado para cancelar esta compra");

        compra.getItems().forEach(item ->
                localidadRepository.incrementarDisponibles(
                        item.getLocalidad().getId(), item.getCantidad()));

        compraRepository.deleteById(id);
    }

    // --- helpers privados ---

    private BigDecimal calcularPrecioConPromocion(Localidad localidad) {
        return promocionRepository
                .findVigenteByEventoId(localidad.getEvento().getId(), LocalDate.now())
                .map(promo -> localidad.getPrecio()
                        .multiply(BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(promo.getDescuento())))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .orElse(localidad.getPrecio());
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
}