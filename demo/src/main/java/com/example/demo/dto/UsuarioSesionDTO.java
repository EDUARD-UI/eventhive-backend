package com.example.demo.dto;

import com.example.demo.enums.NivelUsuario;

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
    private boolean esVerificado;
    private NivelUsuario nivel;
    private Integer cantidadCompras;

    public UsuarioSesionDTO() {
    }

    public UsuarioSesionDTO(Long id, String nombre, String apellido, String correo,
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
