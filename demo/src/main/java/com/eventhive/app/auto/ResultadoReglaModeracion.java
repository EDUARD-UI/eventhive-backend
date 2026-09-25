package com.eventhive.app.auto;

import com.eventhive.app.enums.MotivosRechazos;

// Resultado de UNA regla individual. motivo/detalle van null cuando el veredicto es APROBADO.
public record ResultadoReglaModeracion(VeredictoModeracion veredicto, MotivosRechazos motivo, String detalle) {

    public static ResultadoReglaModeracion ok() {
        return new ResultadoReglaModeracion(VeredictoModeracion.APROBADO, null, null);
    }

    public static ResultadoReglaModeracion revision(MotivosRechazos motivo, String detalle) {
        return new ResultadoReglaModeracion(VeredictoModeracion.REVISION, motivo, detalle);
    }

    public static ResultadoReglaModeracion rechazo(MotivosRechazos motivo, String detalle) {
        return new ResultadoReglaModeracion(VeredictoModeracion.RECHAZADO, motivo, detalle);
    }
}