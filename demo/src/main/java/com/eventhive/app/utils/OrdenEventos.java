package com.eventhive.app.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class OrdenEventos {

    private OrdenEventos() {
    }

    public static Pageable estable(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ordenEstable(pageable.getSort()));
    }

    public static Pageable promocionadosPrimero(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return pageable;
        }
        Sort orden = Sort.by(Sort.Direction.DESC, "promocionado").and(ordenEstable(pageable.getSort()));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), orden);
    }

    private static Sort ordenEstable(Sort solicitado) {
        Sort base = solicitado.isSorted()
                ? solicitado
                : Sort.by(Sort.Direction.ASC, "fecha", "hora");
        return base.getOrderFor("id") == null ? base.and(Sort.by(Sort.Direction.ASC, "id")) : base;
    }
}
