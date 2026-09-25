package com.eventhive.app.auto;

import com.eventhive.app.model.Evento;

// Contrato para cada regla de moderación automática.
// Cada regla nueva es una clase @Component que implementa esto: no se toca el motor.
public interface ReglaModeracion {
    ResultadoReglaModeracion evaluar(Evento evento);
}