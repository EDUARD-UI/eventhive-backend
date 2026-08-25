package com.eventhive.app.dto.request;

import com.eventhive.app.enums.PermisoEvento;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class PermisosOperadorRequest {
    @NotNull
    private Set<PermisoEvento> permisos;
}