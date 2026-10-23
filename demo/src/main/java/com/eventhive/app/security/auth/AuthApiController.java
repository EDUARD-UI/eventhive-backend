package com.eventhive.app.security.auth;

import com.eventhive.app.dto.request.SolicitudVerificacionRequest;
import com.eventhive.app.service.ServiceSolicitudVerificacion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.request.OlvideClaveRequest;
import com.eventhive.app.dto.request.RestablecerClaveRequest;
import com.eventhive.app.service.ServiceRecuperacionClave;
import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.request.LoginRequest;
import com.eventhive.app.dto.request.RefreshRequest;
import com.eventhive.app.dto.request.RegistroRequest;
import com.eventhive.app.dto.response.LoginResponseDTO;
import com.eventhive.app.dto.response.UsuarioSesionDTO;
import com.eventhive.app.service.ServiceAutenticacion;
import com.eventhive.app.service.ServiceUsuario;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthApiController {

    private final ServiceRecuperacionClave serviceRecuperacionClave;
    private final ServiceAutenticacion serviceAutenticacion;
    private final ServiceSolicitudVerificacion serviceSolicitud;
    private final ServiceUsuario serviceUsuario;

    //CONSULTAS
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UsuarioSesionDTO>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("No hay sesión activa"));

        UsuarioSesionDTO usuario = serviceUsuario.obtenerSesionDTO(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("Sesión activa", usuario));
    }

    //LOGEO
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponseDTO response = serviceAutenticacion.autenticar(request.getCorreo(), request.getClave());
        return ResponseEntity.ok(ApiResponse.ok("Login exitoso", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> refresh(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestBody(required = false) RefreshRequest request) {

        String token = null;
        if (request != null && StringUtils.hasText(request.getRefreshToken())) {
            token = request.getRefreshToken().trim();
        } else if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        }

        if (!StringUtils.hasText(token)) {
            throw new BadCredentialsException("El refresh token es requerido");
        }

        LoginResponseDTO response = serviceAutenticacion.refrescarToken(token);
        return ResponseEntity.ok(ApiResponse.ok("Token renovado", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.ok("Sesión cerrada"));
    }

    //REGISTRO DE USUARIOS
    @PostMapping("/registrar-cliente")
    public ResponseEntity<ApiResponse<Void>> registrarCliente(@Valid @RequestBody RegistroRequest request) {
        serviceAutenticacion.registrarCliente(
                request.getNombre(), request.getCorreo(),
                request.getTelefono(), request.getClave());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Registro exitoso"));
    }

    @PostMapping("/registro-organizador")
    public ResponseEntity<ApiResponse<Void>> registrarOrganizadorExpress(
            @RequestBody @Valid SolicitudVerificacionRequest request) {

        serviceSolicitud.registrarOrganizador(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Usuario y pre-registro de organización creados con éxito. Ya puedes ingresar al Dashboard."));
    }

    //GESTION DE CLAVES
    @PostMapping("/olvide-clave")
    public ResponseEntity<ApiResponse<Void>> olvideClave(@Valid @RequestBody OlvideClaveRequest request) {
        serviceRecuperacionClave.solicitar(request.getCorreo());
        return ResponseEntity.ok(ApiResponse.ok(
                "Si el correo está registrado, recibirás un enlace para restablecer tu contraseña."));
    }

    @PostMapping("/restablecer-clave")
    public ResponseEntity<ApiResponse<Void>> restablecerClave(@Valid @RequestBody RestablecerClaveRequest request) {
        serviceRecuperacionClave.restablecer(request.getToken(), request.getClave());
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada correctamente"));
    }

}