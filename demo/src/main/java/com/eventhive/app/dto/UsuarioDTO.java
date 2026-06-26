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
    private boolean esVerificado;

    public UsuarioDTO() {
        this.esVerificado = false; // Default value
    }

    public UsuarioDTO(Long id, String nombre, String apellido, String correo, 
                      String telefono, String rolNombre) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.telefono = telefono;
        this.rolNombre = rolNombre;
        this.esVerificado = false;
    }

    public UsuarioDTO(Long id, String nombre, String apellido, String correo, 
                      String telefono, String rolNombre, boolean esVerificado) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.telefono = telefono;
        this.rolNombre = rolNombre;
        this.esVerificado = esVerificado;
    }
}
