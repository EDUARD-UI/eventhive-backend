package com.eventhive.app.repository.specification;

import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.model.Evento;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class EventoSpecification {

    private EventoSpecification() {
    }

    public static Specification<Evento> conReferencias() {
        return (root, query, cb) -> {
            // Solo aplica el JOIN FETCH cuando la consulta trae datos (no en el COUNT de paginación)
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("categoria", JoinType.INNER);
                root.fetch("organizacion", JoinType.INNER);
                query.distinct(true);
            }
            return cb.conjunction();
        };
    }

    public static Specification<Evento> conTitulo(String titulo) {
        String patron = "%" + titulo.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("titulo")), patron);
    }

    public static Specification<Evento> conCategoria(Long categoriaId) {
        return (root, query, cb) -> cb.equal(root.get("categoria").get("id"), categoriaId);
    }

    public static Specification<Evento> conEstado(EstadoEvento estado) {
        return (root, query, cb) -> cb.equal(root.get("estado"), estado);
    }

    public static Specification<Evento> build(String titulo, Long categoriaId, String estado) {
        Specification<Evento> spec = conReferencias();

        if (titulo != null && !titulo.isBlank()) {
            spec = spec.and(conTitulo(titulo));
        }
        if (categoriaId != null) {
            spec = spec.and(conCategoria(categoriaId));
        }
        if (estado != null && !estado.isBlank()) {
            spec = spec.and(conEstado(parseEstado(estado)));
        }
        return spec;
    }

    private static EstadoEvento parseEstado(String estado) {
        try {
            return EstadoEvento.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Estado inválido: " + estado);
        }
    }
}
