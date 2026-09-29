package com.eventhive.app.enums;

// PENDIENTE_REVISION -> registrada, completando o esperando revisión del RUT
// APROBADA           -> RUT verificado: puede publicar y vender
// SUSPENDIDA         -> RUT rechazado (o suspensión administrativa): bloqueada hasta una nueva verificación
public enum EstadoOrganizacion {
    PENDIENTE_REVISION,
    APROBADA,
    SUSPENDIDA;

    public boolean puedeTransicionarA(EstadoOrganizacion destino) {
        if (destino == null) return false;
        return switch (this) {
            case PENDIENTE_REVISION -> destino == APROBADA || destino == SUSPENDIDA;
            case SUSPENDIDA -> destino == PENDIENTE_REVISION || destino == APROBADA;
            case APROBADA -> destino == SUSPENDIDA;
        };
    }
}
