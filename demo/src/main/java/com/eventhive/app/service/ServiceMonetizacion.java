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

    public BigDecimal calcularComision(BigDecimal total, BigDecimal porcentaje) {
        if (total == null || total.signum() < 0) {
            throw new BusinessException("El total de la compra no es válido");
        }

        if (porcentaje == null || porcentaje.compareTo(BigDecimal.ZERO) < 0 || porcentaje.compareTo(COMISION_PROMOCIONADA) > 0) {
            throw new BusinessException("El porcentaje de comisión no es válido");
        }

        return total.multiply(porcentaje).divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularNetoOrganizador(BigDecimal total, BigDecimal comision) {
        return total.subtract(comision).setScale(2, RoundingMode.HALF_UP);
    }
}