package com.eventhive.app.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "planes_promocion", indexes = {
        @Index(name = "idx_plan_promocion_codigo", columnList = "codigo", unique = true)
})
@Getter
@Setter
public class PlanPromocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "comision_porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal comisionPorcentaje;

    @Column(nullable = false)
    private boolean premium;

    @Column(name = "pauta_redes", nullable = false)
    private boolean pautaRedes;

    @Column(name = "detalle_publicidad", columnDefinition = "TEXT")
    private String detallePublicidad;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
