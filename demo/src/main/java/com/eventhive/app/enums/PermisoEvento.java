package com.eventhive.app.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

// Permisos que el representante puede otorgar a un TRABAJADOR sobre eventos.
public enum PermisoEvento {
    CHECK_IN,
    CREAR_EVENTO,
    EDITAR_EVENTO,
    CANCELAR_EVENTO;

    @JsonCreator
    public static PermisoEvento fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String clean = value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (PermisoEvento p : values()) {
            if (p.name().equalsIgnoreCase(clean)) {
                return p;
            }
        }
        return null;
    }
}
