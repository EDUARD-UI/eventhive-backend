package com.eventhive.app.dto;

import com.eventhive.app.enums.NivelUsuario;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioSesionDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String correo;
    private String telefono;
    private String rolNombre;
    private boolean InsigniaVerificacion;
    private NivelUsuario nivel;
    private Integer cantidadCompras;

    public UsuarioSesionDTO() {
    }

    public UsuarioSesionDTO(Long id, String nombre, String apellido, String correo,
                            String telefono, String rolNombre, boolean InsigniaVerificacion, NivelUsuario nivel, Integer cantidadCompras) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.telefono = telefono;
        this.rolNombre = rolNombre;
        this.InsigniaVerificacion = InsigniaVerificacion;
        this.nivel = nivel;
        this.cantidadCompras = cantidadCompras;
    }
}
