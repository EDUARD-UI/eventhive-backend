package com.eventhive.app.security.users;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(email)
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
