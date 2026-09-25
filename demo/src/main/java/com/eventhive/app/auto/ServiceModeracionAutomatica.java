package com.eventhive.app.auto;

import java.util.List;

import org.springframework.stereotype.Service;

import com.eventhive.app.model.Evento;

import lombok.RequiredArgsConstructor;

/*
 * Orquestador del motor de reglas.
 * Spring inyecta automáticamente TODAS las clases @Component que implementan
 * ReglaModeracion en esta lista, en el orden en que fueron declaradas.
 * Para agregar una regla nueva solo se crea la clase en /reglas, no se modifica esta clase.
 */
@Service
@RequiredArgsConstructor
public class ServiceModeracionAutomatica {

    private final List<ReglaModeracion> reglas;

    public ResultadoReglaModeracion evaluarEvento(Evento evento) {
        ResultadoReglaModeracion hallazgoRevision = null;

        for (ReglaModeracion regla : reglas) {
            ResultadoReglaModeracion resultado = regla.evaluar(evento);

            // Un RECHAZADO corta inmediatamente: no hace falta seguir evaluando.
            if (resultado.veredicto() == VeredictoModeracion.RECHAZADO) {
                return resultado;
            }

            // Guardamos el primer hallazgo que amerite revisión humana, pero seguimos
            // evaluando por si alguna regla posterior encuentra un RECHAZADO.
            if (resultado.veredicto() == VeredictoModeracion.REVISION && hallazgoRevision == null) {
                hallazgoRevision = resultado;
            }
        }

        return hallazgoRevision != null ? hallazgoRevision : ResultadoReglaModeracion.ok();
    }
}