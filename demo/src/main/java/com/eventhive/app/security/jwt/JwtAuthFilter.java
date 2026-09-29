package com.eventhive.app.security.jwt;

import com.eventhive.app.security.users.CustomUserDetailsService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extraerToken(request);

        if (token != null) {
            try {
                // un refresh token ya NO sirve como Bearer: solo type=access autentica
                if (jwtUtils.validarAccessToken(token)) {
                    String correo = jwtUtils.getCorreoDesdeToken(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(correo);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException | IllegalArgumentException | AuthenticationException ex) {
                // Problema del TOKEN: roto, vencido, firma invalida, usuario borrado
                // o inactivo. Se continua como anonimo; que la ruta sea publica o
                // requiera sesion lo decide unicamente authorizeHttpRequests().
                log.debug("Token Bearer invalido, se continua como anonimo: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            } catch (Exception ex) {
                // Fallo del SERVIDOR (BD caida, LazyInitializationException, bug...).
                // No es culpa del token: convertirlo en 401 hace que el frontend cierre
                // la sesion de usuarios validos y oculta el error real. Se registra con
                // stack trace y se responde 500.
                log.error("Error inesperado al autenticar {} {}", request.getMethod(),
                        request.getRequestURI(), ex);
                SecurityContextHolder.clearContext();
                responderErrorInterno(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    // Se escribe la respuesta directamente: sendError() provocaria un dispatch a /error,
    // que Spring Security volveria a proteger y terminaria devolviendo 401.
    private void responderErrorInterno(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"mensaje\":\"Error interno al validar la sesión\"}");
    }

    private String extraerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
