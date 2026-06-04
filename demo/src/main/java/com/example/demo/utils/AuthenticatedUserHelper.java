package com.example.demo.utils;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.demo.exception.BusinessException;
import com.example.demo.model.Usuario;
import com.example.demo.security.users.UsuarioPrincipal;

@Component
public class AuthenticatedUserHelper {

    public String getCorreoAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken)
            throw new BusinessException("Debe iniciar sesión para continuar");
        return auth.getName();
    }

    public Usuario usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken)
            throw new BusinessException("Debe iniciar sesión para continuar");
        
        if (auth.getPrincipal() instanceof UsuarioPrincipal usuarioPrincipal) {
            return usuarioPrincipal.getUsuario();
        }
        
        throw new BusinessException("No se puede obtener el usuario autenticado");
    }
}
