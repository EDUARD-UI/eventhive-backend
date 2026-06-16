package com.example.demo.service;

import org.springframework.stereotype.Service;

import com.example.demo.enums.NivelUsuario;

@Service
public class ServiceFidelizacion {

    public NivelUsuario calcularNivel(Integer compras) {
        if (compras == null) compras = 0;
        if (compras >= 30) return NivelUsuario.DIAMANTE;
        if (compras >= 15) return NivelUsuario.ORO;
        if (compras >= 5)  return NivelUsuario.PLATA;
        return NivelUsuario.BRONCE;
    }

    public long obtenerHorasAnticipacion(NivelUsuario nivel) {
        if (nivel == null) return 0L;
        return switch (nivel) {
            case DIAMANTE -> 48L;
            case ORO      -> 24L;
            case PLATA    -> 12L;
            default       -> 0L;
        };
    }
}
