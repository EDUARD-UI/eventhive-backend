package com.eventhive.app.dto.response;

import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.model.Localidad;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class EventoDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    private String lugar;
    private String foto;
    private LocalDate fecha;
    private LocalTime hora;
    private EstadoEvento estado;
    private Double latitud;
    private Double longitud;
    private EventoCategoriaDTO categoria;
    private EventoOrganizacionDTO organizacion;
    private List<Localidad> localidades;
}