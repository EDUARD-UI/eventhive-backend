package com.eventhive.app.dto.response;

import java.util.Set;

import com.eventhive.app.enums.PermisoEvento;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OperadorDTO {
    private Long id;
    private String nombreCompleto;
    private String correo;
    private Set<PermisoEvento> permisosEvento;
}