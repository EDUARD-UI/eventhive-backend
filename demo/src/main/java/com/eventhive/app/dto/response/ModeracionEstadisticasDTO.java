package com.eventhive.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ModeracionEstadisticasDTO {

    private long publicados;
    private long pendientesRevision;
    private long enCorreccion;
    private long rechazados;

    // Campos para compatibilidad con consumidores anteriores
    private long revisados;
    private long aprobados;
    private long correccionesSolicitadas;

    public ModeracionEstadisticasDTO(long publicados, long pendientesRevision, long enCorreccion, long rechazados) {
        this.publicados = publicados;
        this.pendientesRevision = pendientesRevision;
        this.enCorreccion = enCorreccion;
        this.rechazados = rechazados;
        this.aprobados = publicados;
        this.revisados = pendientesRevision;
        this.correccionesSolicitadas = enCorreccion;
    }
}