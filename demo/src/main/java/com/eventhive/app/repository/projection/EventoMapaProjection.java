package com.eventhive.app.repository.projection;

public interface EventoMapaProjection {
    Long getId();
    String getTitulo();
    String getDescripcion();
    String getCategoriaNombre();
    Double getLatitud();
    Double getLongitud();
}