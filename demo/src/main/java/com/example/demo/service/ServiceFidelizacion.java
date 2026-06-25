package com.example.demo.service;

import org.springframework.stereotype.Service;

import com.example.demo.enums.NivelUsuario;

@Service
public class ServiceFidelizacion {

    //cantidad de eventos comprados para subir de nivel
    private static final int UMBRAL_PLATA    = 5;
    private static final int UMBRAL_ORO      = 15;
    private static final int UMBRAL_DIAMANTE = 30;

    // Horas de acceso anticipado por nivel
    private static final long HORAS_PLATA    = 12L;
    private static final long HORAS_ORO      = 24L;
    private static final long HORAS_DIAMANTE = 48L;

    public NivelUsuario calcularNivel(Integer compras) {
        if (compras == null || compras < 0) compras = 0;

        if (compras >= UMBRAL_DIAMANTE) return NivelUsuario.DIAMANTE;
        if (compras >= UMBRAL_ORO)      return NivelUsuario.ORO;
        if (compras >= UMBRAL_PLATA)    return NivelUsuario.PLATA;

        return NivelUsuario.BRONCE;
    }

    //Devuelve cuántas horas antes puede ver eventos publicados un usuario
    public long obtenerHorasAnticipacion(NivelUsuario nivel) {
        if (nivel == null) return 0L;

        return switch (nivel) {
            case DIAMANTE -> HORAS_DIAMANTE;
            case ORO      -> HORAS_ORO;
            case PLATA    -> HORAS_PLATA;
            default       -> 0L;
        };
    }

    //Indica si una cantidad de compras representa un cambio de nivel
    public boolean hayCambioDeNivel(NivelUsuario nivelActual, Integer comprasTotales) {
        NivelUsuario nivelCalculado = calcularNivel(comprasTotales);
        return nivelCalculado != nivelActual;
    }
}