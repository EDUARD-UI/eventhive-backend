package com.eventhive.app.enums;

// Nivel de confianza de una organización: define su límite de eventos activos y si se le exige moderación
public enum NivelOrganizador {

    NIVEL_1(3, false),
    NIVEL_2(5, false),
    NIVEL_3(10, true);

    private final int maxEventosActivos;
    private final boolean publicacionAutomatica;

    NivelOrganizador(int maxEventosActivos, boolean publicacionAutomatica) {
        this.maxEventosActivos = maxEventosActivos;
        this.publicacionAutomatica = publicacionAutomatica;
    }

    public int maxEventosActivos() {
        return maxEventosActivos;
    }

    // Nivel 3: el evento se publica directo, sin pasar por PENDIENTE_REVISION
    public boolean permitePublicacionAutomatica() {
        return publicacionAutomatica;
    }

    // Siguiente nivel en la escala; NIVEL_3 ya es el tope
    public NivelOrganizador siguiente() {
        return switch (this) {
            case NIVEL_1 -> NIVEL_2;
            case NIVEL_2 -> NIVEL_3;
            case NIVEL_3 -> NIVEL_3;
        };
    }

    public boolean esMaximo() {
        return this == NIVEL_3;
    }
}
