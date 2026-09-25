package com.eventhive.app.auto.reglas;

import org.springframework.stereotype.Component;

import com.eventhive.app.enums.MotivosRechazos;
import com.eventhive.app.model.Evento;
import com.eventhive.app.auto.ReglaModeracion;
import com.eventhive.app.auto.ResultadoReglaModeracion;

@Component
public class ReglaImagenRequerida implements ReglaModeracion {

    @Override
    public ResultadoReglaModeracion evaluar(Evento evento) {
        if (evento.getFoto() == null || evento.getFoto().isBlank()) {
            // Va a REVISION (no a RECHAZO): puede ser válido, pero un humano decide.
            return ResultadoReglaModeracion.revision(
                    MotivosRechazos.IMAGEN_NO_PERMITIDA,
                    "El evento no tiene imagen principal; requiere revisión manual.");
        }
        return ResultadoReglaModeracion.ok();
    }
}