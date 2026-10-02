package com.eventhive.app.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Vista de usuario para el panel administrativo: sin correo ni estado.
// "organizacion" es null cuando el usuario no está vinculado a ninguna (el front muestra "Sin organización").
@Getter
@Setter
@NoArgsConstructor
public class UsuarioAdminDTO {

    private Long id;
    private String nombre;
    private String telefono;
    private String rolNombre;
    private String imagenPerfil;
    private OrganizacionResumenDTO organizacion;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class OrganizacionResumenDTO {
        private Long id;
        private String razonSocial;

        public OrganizacionResumenDTO(Long id, String razonSocial) {
            this.id = id;
            this.razonSocial = razonSocial;
        }
    }
}
