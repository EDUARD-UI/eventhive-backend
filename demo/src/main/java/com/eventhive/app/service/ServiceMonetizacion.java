package com.eventhive.app.service;

import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.model.Evento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ServiceMonetizacion {

    private static final BigDecimal CIEN = new BigDecimal("100.00");
    public static final BigDecimal COMISION_NORMAL = new BigDecimal("7.00");
    private static final BigDecimal COMISION_PROMOCIONADA = new BigDecimal("100.00");

    public BigDecimal obtenerPorcentajeComision(Evento evento) {
        if (evento == null) {
            throw new BusinessException("El evento es obligatorio");
        }

        if (Boolean.TRUE.equals(evento.getPromocionado())) {
            return evento.getComisionPromocionPorcentaje() != null
                    ? evento.getComisionPromocionPorcentaje()
                    : new BigDecimal("9.00");
        }

        return COMISION_NORMAL;
    }

    public BigDecimal calcularComision(BigDecimal baseTotal, BigDecimal porcentaje) {
        if (baseTotal == null || baseTotal.signum() < 0) {
            throw new BusinessException("El importe base de la compra no es válido");
        }

        if (porcentaje == null || porcentaje.compareTo(BigDecimal.ZERO) < 0 || porcentaje.compareTo(COMISION_PROMOCIONADA) > 0) {
            throw new BusinessException("El porcentaje de comisión no es válido");
        }

        return baseTotal.multiply(porcentaje).divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularTotalComprador(BigDecimal baseTotal, BigDecimal comision) {
        if (baseTotal == null || comision == null) {
            throw new BusinessException("Los importes no pueden ser nulos");
        }
        return baseTotal.add(comision).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularNetoOrganizador(BigDecimal baseTotal) {
        if (baseTotal == null || baseTotal.signum() < 0) {
            throw new BusinessException("El importe base de la organización no es válido");
        }
        return baseTotal.setScale(2, RoundingMode.HALF_UP);
    }

    @Deprecated
    public BigDecimal calcularNetoOrganizador(BigDecimal total, BigDecimal comision) {
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}