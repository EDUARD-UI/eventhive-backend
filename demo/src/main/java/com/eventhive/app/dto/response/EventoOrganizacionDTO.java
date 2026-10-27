package com.eventhive.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventoOrganizacionDTO {
    private Long id;
    private String nombre;
    private String urlLogo;

    public EventoOrganizacionDTO(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public String getUrlImagen() {
        return urlLogo;
    }

    public String getImagen() {
        return urlLogo;
    }
}