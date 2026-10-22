package com.eventhive.app.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.request.CorreoAdministrativoRequest;
import com.eventhive.app.dto.request.InvitacionRolRequest;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.service.ServiceCorreo;
import com.eventhive.app.service.ServiceInvitacionRol;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/correos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminCorreosApiController {

    private final ServiceCorreo serviceCorreo;
    private final ServiceInvitacionRol serviceInvitacionRol;
    private final AuthenticatedUserHelper authHelper;

    @PostMapping("/enviar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> enviarCorreo(
            @Valid @RequestBody CorreoAdministrativoRequest request) {
        Usuario admin = authHelper.usuarioAutenticado();
        serviceCorreo.enviarCorreoAdministrativo(
                request.getDestinatario(),
                request.getAsunto(),
                request.getMensaje(),
                admin
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Solicitud de envío de correo administrativo aceptada",
                Map.of(
                        "destinatario", request.getDestinatario(),
                        "iniciadoPor", admin.getCorreo(),
                        "estado", "ACEPTADO"
                )
        ));
    }

    @PostMapping("/invitar")
    public ResponseEntity<ApiResponse<Void>> invitarRol(
            @Valid @RequestBody InvitacionRolRequest request) {
        serviceInvitacionRol.invitar(request.getCorreo(), request.getRol());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Invitación administrativa enviada por correo"));
    }
}
