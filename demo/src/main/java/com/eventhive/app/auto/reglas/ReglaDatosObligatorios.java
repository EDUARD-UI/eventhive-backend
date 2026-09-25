package com.eventhive.app.auto.reglas;

import com.eventhive.app.auto.ResultadoReglaModeracion;
import org.springframework.stereotype.Component;

import com.eventhive.app.enums.MotivosRechazos;
import com.eventhive.app.model.Evento;
import com.eventhive.app.auto.ReglaModeracion;
import com.eventhive.app.auto.ResultadoReglaModeracion;

@Component
public class ReglaDatosObligatorios implements ReglaModeracion {

    @Override
    public ResultadoReglaModeracion evaluar(Evento evento) {
        boolean incompleto = esVacio(evento.getTitulo())
                || esVacio(evento.getDescripcion())
                || esVacio(evento.getLugar())
                || evento.getFecha() == null
                || evento.getHora() == null
                || evento.getCategoria() == null;

        if (incompleto) {
            return ResultadoReglaModeracion.rechazo(
                    MotivosRechazos.INFORMACION_INCOMPLETA,
                    "Faltan campos obligatorios del evento (título, descripción, lugar, fecha, hora o categoría).");
        }
        return ResultadoReglaModeracion.ok();
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}