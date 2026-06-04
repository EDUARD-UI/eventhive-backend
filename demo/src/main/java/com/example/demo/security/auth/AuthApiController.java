package com.example.demo.security.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.UsuarioSesionDTO;
import com.example.demo.service.ServiceAutenticacion;
import com.example.demo.service.ServiceUsuario;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthApiController {

    private final ServiceAutenticacion serviceAutenticacion;
    private final ServiceUsuario serviceUsuario;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UsuarioSesionDTO>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("No hay sesión activa"));
        }

        try {
            // Obtener datos del usuario autenticado
            UsuarioSesionDTO usuario = serviceUsuario.obtenerSesionDTO(auth.getName());
            
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Usuario no encontrado"));
            }
            
            return ResponseEntity.ok(ApiResponse.ok("Sesión activa", usuario));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al obtener datos de sesión"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UsuarioSesionDTO>> login(
            @RequestParam String correo,
            @RequestParam String clave,
            HttpSession session) {
        try {
            Authentication auth = serviceAutenticacion.autenticar(correo, clave);
            SecurityContextHolder.getContext().setAuthentication(auth);
            session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
            );
            
            // Obtener datos del usuario para retornar
            UsuarioSesionDTO usuario = serviceUsuario.obtenerSesionDTO(correo);
            
            return ResponseEntity.ok(ApiResponse.ok("Login exitoso", usuario));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Credenciales inválidas"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpSession session) {
        session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.ok("Sesión cerrada"));
    }

    @PostMapping("/registrar-cliente")
    public ResponseEntity<ApiResponse<Void>> registrarCliente(
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam String correo,
            @RequestParam String telefono,
            @RequestParam String clave) {
        try {
            serviceAutenticacion.registrarCliente(nombre, apellido, correo, telefono, clave);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Registro exitoso"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/registrar-organizador")
    public ResponseEntity<ApiResponse<Void>> registrarOrganizador(
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam String correo,
            @RequestParam String telefono,
            @RequestParam String clave) {
        try {
            serviceAutenticacion.registrarOrganizador(nombre, apellido, correo, telefono, clave);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Registro exitoso"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}
