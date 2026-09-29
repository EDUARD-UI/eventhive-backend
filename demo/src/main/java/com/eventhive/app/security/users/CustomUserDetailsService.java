package com.eventhive.app.security.users;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    // Este metodo tambien se ejecuta desde JwtAuthFilter, es decir, fuera de cualquier
    // transaccion y con open-in-view=false. Usuario.rol es LAZY: si el rol no viene
    // cargado en la misma consulta, UsuarioPrincipal.getAuthorities() lanza
    // LazyInitializationException y la peticion queda sin autenticar (401).
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreoConRol(email)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));

        if (!usuario.isActivo()) {
            throw new UsernameNotFoundException("Credenciales inválidas");
        }

        if (usuario.getRol() == null) {
            throw new UsernameNotFoundException("Credenciales inválidas");
        }

        return new UsuarioPrincipal(usuario);
    }
}
