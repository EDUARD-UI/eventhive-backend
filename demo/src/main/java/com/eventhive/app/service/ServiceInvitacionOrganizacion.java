package com.eventhive.app.service;

import com.eventhive.app.dto.response.InvitacionOrganizacionDTO;
import com.eventhive.app.enums.EstadoInvitacion;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.InvitacionOrganizacion;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.InvitacionOrganizacionRepository;
import com.eventhive.app.repository.RolesRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ServiceInvitacionOrganizacion {

    private final InvitacionOrganizacionRepository invitacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolesRepository rolesRepository;
    private final AuthenticatedUserHelper authHelper;

    //CONSULTAS
    @Transactional(readOnly = true)
    public Page<InvitacionOrganizacionDTO> misInvitacionesPendientes(Pageable pageable) {
        Usuario usuario = authHelper.usuarioAutenticado();
        return invitacionRepository
                .findByCorreoInvitadoAndEstado(usuario.getCorreo(), EstadoInvitacion.PENDIENTE, pageable)
                .map(this::toDTO);
    }

    // Historial de invitaciones enviadas por la organización del representante
    @Transactional(readOnly = true)
    public Page<InvitacionOrganizacionDTO> listarInvitacionesOrganizacion(Pageable pageable) {
        Organizacion organizacion = organizacionDelRepresentante();
        return invitacionRepository.findByOrganizacionId(organizacion.getId(), pageable).map(this::toDTO);
    }

    //ACCIONES DE INVITACION
    @Transactional
    public void invitar(String correoInvitado) {
        Usuario representante = authHelper.usuarioAutenticado();
        Organizacion organizacion = representante.getOrganizacion();
        if (organizacion == null || !organizacion.getRepresentante().getId().equals(representante.getId()))
            throw new BusinessException("Solo el representante puede invitar trabajadores");

        Usuario candidato = usuarioRepository.findByCorreo(correoInvitado)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una cuenta con ese correo"));

        if (!"CLIENTE".equalsIgnoreCase(candidato.getRol().getNombre()))
            throw new BusinessException("El invitado debe tener una cuenta de tipo CLIENTE");

        if (candidato.getOrganizacion() != null)
            throw new BusinessException("El usuario ya pertenece a una organización");

        if (invitacionRepository.existsByCorreoInvitadoAndEstado(correoInvitado, EstadoInvitacion.PENDIENTE))
            throw new BusinessException("Ya existe una invitación pendiente para este correo");

        InvitacionOrganizacion invitacion = new InvitacionOrganizacion();
        invitacion.setOrganizacion(organizacion);
        invitacion.setCorreoInvitado(correoInvitado);
        invitacion.setInvitadoPor(representante);
        invitacionRepository.save(invitacion);
    }

    @Transactional
    public void aceptar(Long invitacionId) {
        Usuario operador = authHelper.usuarioAutenticado();
        InvitacionOrganizacion invitacion = obtenerPendientePara(invitacionId, operador.getCorreo());

        if (operador.getOrganizacion() != null)
            throw new BusinessException("Ya perteneces a una organización");

        operador.setOrganizacion(invitacion.getOrganizacion());
        operador.setRol(rolesRepository.findByNombre("OPERADOR")
                .orElseThrow(() -> new ResourceNotFoundException("Rol OPERADOR no configurado")));
        usuarioRepository.save(operador);

        invitacion.setEstado(EstadoInvitacion.ACEPTADA);
        invitacion.setFechaRespuesta(LocalDateTime.now());
        invitacionRepository.save(invitacion);
    }

    @Transactional
    public void rechazar(Long invitacionId) {
        Usuario usuario = authHelper.usuarioAutenticado();
        InvitacionOrganizacion invitacion = obtenerPendientePara(invitacionId, usuario.getCorreo());
        invitacion.setEstado(EstadoInvitacion.RECHAZADA);
        invitacion.setFechaRespuesta(LocalDateTime.now());
        invitacionRepository.save(invitacion);
    }

    //METODOS AUXILIARES Y DE MAPEO
    private InvitacionOrganizacion obtenerPendientePara(Long id, String correo) {
        InvitacionOrganizacion invitacion = invitacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invitación no encontrada"));
        if (!invitacion.getCorreoInvitado().equalsIgnoreCase(correo))
            throw new ResourceNotFoundException("Invitación no encontrada");
        if (invitacion.getEstado() != EstadoInvitacion.PENDIENTE)
            throw new BusinessException("La invitación ya fue resuelta");
        return invitacion;
    }

    private Organizacion organizacionDelRepresentante() {
        Usuario representante = authHelper.usuarioAutenticado();
        Organizacion organizacion = representante.getOrganizacion();
        if (organizacion == null || !organizacion.getRepresentante().getId().equals(representante.getId()))
            throw new BusinessException("No tienes una organización para administrar");
        return organizacion;
    }

    private InvitacionOrganizacionDTO toDTO(InvitacionOrganizacion i) {
        InvitacionOrganizacionDTO dto = new InvitacionOrganizacionDTO();
        dto.setId(i.getId());
        dto.setCorreoInvitado(i.getCorreoInvitado());
        dto.setOrganizacionNombre(i.getOrganizacion().getRazonSocial());
        dto.setInvitadoPorNombre(i.getInvitadoPor().getNombreCompleto());
        dto.setEstado(i.getEstado());
        dto.setFechaInvitacion(i.getFechaInvitacion());
        dto.setFechaRespuesta(i.getFechaRespuesta());
        return dto;
    }
}
