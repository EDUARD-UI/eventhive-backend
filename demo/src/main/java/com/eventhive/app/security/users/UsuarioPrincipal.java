package com.eventhive.app.security.users;

import com.eventhive.app.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

public class UsuarioPrincipal implements UserDetails {
    
    private final Usuario usuario;

    public UsuarioPrincipal(Usuario usuario) {
        this.usuario = usuario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        
        if (usuario.getRol() != null) {
            String rolNombre = usuario.getRol().getNombre();
            if (rolNombre != null && !rolNombre.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + rolNombre));
            }
        }
        
        return authorities;
    }

    @Override
    public String getPassword() {
        return usuario.getClave();
    }

    @Override
    public String getUsername() {
        return usuario.getCorreo();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return usuario.getBloqueadoHasta() == null
                || usuario.getBloqueadoHasta().isBefore(LocalDateTime.now());
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
