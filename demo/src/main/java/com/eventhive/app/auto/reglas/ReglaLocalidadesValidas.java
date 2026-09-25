package com.eventhive.app.auto.reglas;

import org.springframework.stereotype.Component;

import com.eventhive.app.enums.MotivosRechazos;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Localidad;
import com.eventhive.app.auto.ReglaModeracion;
import com.eventhive.app.auto.ResultadoReglaModeracion;

@Component
public class ReglaLocalidadesValidas implements ReglaModeracion {

    @Override
    public ResultadoReglaModeracion evaluar(Evento evento) {
        var localidades = evento.getLocalidades();

        boolean invalido = localidades == null
                || localidades.isEmpty()
                || localidades.stream().anyMatch(this::esLocalidadInvalida);

        if (invalido) {
            return ResultadoReglaModeracion.rechazo(
                    MotivosRechazos.LOCALIDADES_INVALIDAS,
                    "El evento no tiene al menos una localidad con capacidad y precio válidos.");
        }
        return ResultadoReglaModeracion.ok();
    }

    private boolean esLocalidadInvalida(Localidad localidad) {
        return localidad.getCapacidad() <= 0
                || localidad.getPrecio() == null
                || localidad.getPrecio().signum() < 0;
    }
}