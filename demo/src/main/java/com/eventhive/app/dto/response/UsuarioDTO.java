package com.eventhive.app.dto.response;

import java.util.HashSet;
import java.util.Set;

import com.eventhive.app.enums.PermisoEvento;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UsuarioDTO {
    private Long id;
    private String nombre;
    private String correo;
    private String telefono;
    private String rolNombre;
    private String imagenPerfil;
    private Long organizacionId;
    private String organizacionNombre;
    private Set<PermisoEvento> permisosEvento = new HashSet<>();

    public String getUrlImagenPerfil() {
        return imagenPerfil;
    }

    public Set<PermisoEvento> getPermisos() {
        return permisosEvento;
    }

    public void setPermisos(Set<PermisoEvento> permisos) {
        this.permisosEvento = permisos != null ? permisos : new HashSet<>();
    }
}
