package com.eventhive.app.dto.response;

import java.time.LocalDateTime;

import com.eventhive.app.enums.NivelOrganizador;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizacionPublicaDTO {
    private Long id;
    private String razonSocial;
    private String representante;
    private LocalDateTime fechaCreacion;
    private Double promedioRating;
    private Integer totalValoraciones;
    private Integer totalSeguidores;
    private Integer totalEventosCreados;
    private NivelOrganizador nivel;
}