package com.eventhive.app.utils;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserHelper {

    private final UsuarioRepository usuarioRepository;

    public String getCorreoAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken)
            throw new BusinessException("Debe iniciar sesión para continuar");
        return auth.getName();
    }

    // El principal del SecurityContext solo garantiza el rol (lo justo para autenticar).
    // Los servicios necesitan tambien organizacion y permisos, asi que se recarga el
    // usuario con todo inicializado. Si el llamador ya tiene una transaccion, se une a
    // ella y la entidad queda gestionada; si no, igualmente vuelve completa.
    @Transactional(readOnly = true)
    public Usuario usuarioAutenticado() {
        String correo = getCorreoAutenticado();
        return usuarioRepository.findByCorreoConContexto(correo)
                .orElseThrow(() -> new BusinessException("No se puede obtener el usuario autenticado"));
    }
}
