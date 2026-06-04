package com.example.demo.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.UsuarioSesionDTO;
import com.example.demo.model.Usuario;
import com.example.demo.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pagos")
public class PagosApiController {

    private final AuthenticatedUserHelper authHelper;

    @GetMapping
    public ResponseEntity<ApiResponse<UsuarioSesionDTO>> mostrarPago() {
        Usuario usuario = authHelper.usuarioAutenticado();

        UsuarioSesionDTO dto = new UsuarioSesionDTO();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombre());
        dto.setApellido(usuario.getApellido());
        dto.setCorreo(usuario.getCorreo());
        dto.setTelefono(usuario.getTelefono());
        if (usuario.getRol() != null) dto.setRolNombre(usuario.getRol().getNombre());

        return ResponseEntity.ok(ApiResponse.ok("Usuario autenticado", dto));
    }
}