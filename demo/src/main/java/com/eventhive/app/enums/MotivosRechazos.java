package com.eventhive.app.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MotivosRechazos {

    INFORMACION_INCOMPLETA(
            "INFORMACION_INCOMPLETA",
            "La información del evento está incompleta."
    ),

    IMAGEN_NO_PERMITIDA(
            "IMAGEN_NO_PERMITIDA",
            "La imagen no cumple las políticas."
    ),

    FALTA_PERMISO_LEGAL(
            "FALTA_PERMISO_LEGAL",
            "Falta adjuntar el permiso legal requerido."
    ),

    CATEGORIA_INCORRECTA(
            "CATEGORIA_INCORRECTA",
            "La categoría seleccionada no corresponde al evento."
    ),

    INCUMPLE_POLITICAS(
            "INCUMPLE_POLITICAS",
            "El evento incumple las políticas de la plataforma."
    ),

    OTRO(
            "OTRO",
            "Otro motivo."
    );

    private final String codigo;
    private final String descripcion;
}
