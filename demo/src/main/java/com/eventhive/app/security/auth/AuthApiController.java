package com.eventhive.app.security.auth;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.response.LoginResponseDTO;
import com.eventhive.app.dto.response.UsuarioSesionDTO;
import com.eventhive.app.dto.request.LoginRequest;
import com.eventhive.app.dto.request.RefreshRequest;
import com.eventhive.app.dto.request.RegistroRequest;
import com.eventhive.app.service.ServiceAutenticacion;
import com.eventhive.app.service.ServiceUsuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthApiController {

    private final ServiceAutenticacion serviceAutenticacion;
    private final ServiceUsuario       serviceUsuario;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UsuarioSesionDTO>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("No hay sesión activa"));

        UsuarioSesionDTO usuario = serviceUsuario.obtenerSesionDTO(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("Sesión activa", usuario));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@RequestBody LoginRequest request) {
        LoginResponseDTO response = serviceAutenticacion.autenticar(request.getCorreo(), request.getClave());
        return ResponseEntity.ok(ApiResponse.ok("Login exitoso", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> refresh(@Valid @RequestBody RefreshRequest request) {
        LoginResponseDTO response = serviceAutenticacion.refrescarToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Token renovado", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.ok("Sesión cerrada"));
    }

    @PostMapping("/registrar-cliente")
    public ResponseEntity<ApiResponse<Void>> registrarCliente(@RequestBody RegistroRequest request) {
        serviceAutenticacion.registrarCliente(
                request.getNombre(), request.getCorreo(),
                request.getTelefono(), request.getClave());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Registro exitoso"));
    }

    @PostMapping("/registrar-organizacion")
    public ResponseEntity<ApiResponse<Void>> registrarOrganizacion(@RequestBody RegistroRequest request) {
        serviceAutenticacion.registrarOrganizacion(
                request.getNombre(), request.getCorreo(),
                request.getTelefono(), request.getClave());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Registro exitoso"));
    }
}