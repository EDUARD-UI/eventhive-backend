package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OrganizadorDashboardDTO {
    private int totalEventos;
    private int totalLocalidades;
    private int totalPromociones;
    private boolean esVerificado;

    public OrganizadorDashboardDTO() {
    }

    public OrganizadorDashboardDTO(int totalEventos, int totalLocalidades, 
                                   int totalPromociones, boolean esVerificado) {
        this.totalEventos = totalEventos;
        this.totalLocalidades = totalLocalidades;
        this.totalPromociones = totalPromociones;
        this.esVerificado = esVerificado;
    }
}
