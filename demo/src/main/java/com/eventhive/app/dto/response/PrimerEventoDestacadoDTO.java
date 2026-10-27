package com.eventhive.app.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrimerEventoDestacadoDTO {
    private Long id;
    private String nombre;
    private String urlImagen;
    private LocalDateTime fechaCreacion;
    private LocalDate fecha; // Fecha de ejecución
    private LocalTime hora;

    // Alias para compatibilidad con clientes existentes
    public String getTitulo() {
        return nombre;
    }

    public String getFoto() {
        return urlImagen;
    }
}
