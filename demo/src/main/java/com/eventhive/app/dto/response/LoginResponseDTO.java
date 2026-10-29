package com.eventhive.app.dto.response;

import java.util.Set;

import com.eventhive.app.enums.PermisoEvento;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponseDTO {
    private Long id;
    private String accessToken;
    private String refreshToken;
    private String tipo;
    private String correo;
    private String rol;
    private Long organizacionId;
    private String organizacionNombre;
    private Set<PermisoEvento> permisosEvento;

    public Set<PermisoEvento> getPermisos() {
        return permisosEvento;
    }
}
