package com.eventhive.app.config;

import com.eventhive.app.model.Rol;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.RolesRepository;
import com.eventhive.app.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RolesRepository rolesRepository;

    @Override
    public void run(String... args) throws Exception {
        if (usuarioRepository.count() == 0) {
            Rol rol = rolesRepository.findByNombre("ADMINISTRADOR").orElse(null);
            if (rol != null){
                Usuario admin = new Usuario();
                admin.setNombreCompleto("AdministradorGlobal");
                admin.setClave(passwordEncoder.encode("global12bc"));
                admin.setFechaCreacion(java.time.LocalDateTime.now());
                admin.setRol(rol);
                admin.setCorreo("eduardestf20@gmail.com");
                admin.setIntentosFallidos(0);
                usuarioRepository.save(admin);
                System.out.println("Usuario administrador creado con éxito.");
            }
        }


    }
}
