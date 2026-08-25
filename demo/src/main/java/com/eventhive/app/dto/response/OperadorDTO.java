package com.eventhive.app.dto.response;

import com.eventhive.app.enums.PermisoEvento;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class OperadorDTO {
    private Long id;
    private String nombreCompleto;
    private String correo;
    private Set<PermisoEvento> permisosEvento;
}