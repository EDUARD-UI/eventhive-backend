package com.eventhive.app.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String correo;
    private String telefono;
    private String rolNombre;
    private boolean InsigniaVerificacion;

    public UsuarioDTO() {
        this.InsigniaVerificacion = false;
    }

    public UsuarioDTO(Long id, String nombre, String apellido, String correo, 
                      String telefono, String rolNombre) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.telefono = telefono;
        this.rolNombre = rolNombre;
        this.InsigniaVerificacion = false;
    }

    public UsuarioDTO(Long id, String nombre, String apellido, String correo, 
                      String telefono, String rolNombre, boolean InsigniaVerificacion) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.telefono = telefono;
        this.rolNombre = rolNombre;
        this.InsigniaVerificacion = InsigniaVerificacion;
    }
}
