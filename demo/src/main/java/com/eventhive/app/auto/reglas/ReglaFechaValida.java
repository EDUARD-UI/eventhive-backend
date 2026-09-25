package com.eventhive.app.auto.reglas;

import java.time.LocalDateTime;

import com.eventhive.app.auto.ResultadoReglaModeracion;
import org.springframework.stereotype.Component;

import com.eventhive.app.enums.MotivosRechazos;
import com.eventhive.app.model.Evento;
import com.eventhive.app.auto.ReglaModeracion;
import com.eventhive.app.auto.ResultadoReglaModeracion;

@Component
public class ReglaFechaValida implements ReglaModeracion {

    @Override
    public ResultadoReglaModeracion evaluar(Evento evento) {
        if (evento.getFecha() == null || evento.getHora() == null) {
            return ResultadoReglaModeracion.ok(); // ya lo cubre ReglaDatosObligatorios
        }

        LocalDateTime fechaHoraEvento = LocalDateTime.of(evento.getFecha(), evento.getHora());
        if (fechaHoraEvento.isBefore(LocalDateTime.now())) {
            return ResultadoReglaModeracion.rechazo(
                    MotivosRechazos.FECHA_INVALIDA,
                    "La fecha y hora del evento ya pasaron.");
        }
        return ResultadoReglaModeracion.ok();
    }
}
