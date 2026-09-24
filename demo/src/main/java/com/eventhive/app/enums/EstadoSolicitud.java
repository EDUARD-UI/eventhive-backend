package com.eventhive.app.enums;

public enum EstadoSolicitud {
    INCOMPLETA,          // Datos guardados, falta subir el RUT
    PENDIENTE,           // RUT subido, esperando revisión del Admin
    CORRECCION_SOLICITADA,
    APROBADA,
    RECHAZADA
}
