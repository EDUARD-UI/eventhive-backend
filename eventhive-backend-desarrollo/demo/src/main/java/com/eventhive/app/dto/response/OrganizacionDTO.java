package com.eventhive.app.dto.response;

import com.eventhive.app.enums.NivelOrganizador;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OrganizacionDTO {

    private Long id;

    // Datos legales
    private String razonSocial;
    private String nit;
    private String representanteLegal;
    private String urlRut;
    private LocalDateTime fechaCreacion;

    // Estadísticas
    private Double promedioRating;
    private Integer totalValoraciones;
    private Integer totalSeguidores;
    private Integer totalEventosCreados;
    private Integer eventosFinalizados;
    private Integer eventosRechazados;
    private NivelOrganizador nivel;
}
