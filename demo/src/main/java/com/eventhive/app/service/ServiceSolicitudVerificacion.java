package com.eventhive.app.service;

import com.eventhive.app.dto.response.SolicitudVerificacionDTO;
import com.eventhive.app.dto.request.SolicitudVerificacionRequest;
import com.eventhive.app.enums.EstadoSolicitud;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.SolicitudVerificacion;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.OrganizacionRepository;
import com.eventhive.app.repository.RolesRepository;
import com.eventhive.app.repository.SolicitudVerificacionRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ServiceSolicitudVerificacion {

    private final SolicitudVerificacionRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacionRepository organizacionRepository;
    private final RolesRepository rolesRepository;
    private final AuthenticatedUserHelper authHelper;
    private final SupabaseStorageService storageService;

    @Transactional
    public void crearSolicitud(SolicitudVerificacionRequest request, MultipartFile archivoRut) {
        Usuario representante = authHelper.usuarioAutenticado();

        if (representante.getOrganizacion() != null)
            throw new BusinessException("Esta cuenta ya pertenece a una organización");

        if (solicitudRepository.existsByRepresentanteIdAndEstado(representante.getId(), EstadoSolicitud.PENDIENTE))
            throw new BusinessException("Ya tiene una solicitud pendiente de revisión");

        // Se valida que no esté ya usado como contacto de otra organización.
        if (organizacionRepository.existsByCorreoContacto(request.getCorreoEmpresarial()))
            throw new BusinessException("Ese correo empresarial ya está en uso");

        String urlRut = null;
        if (archivoRut != null && !archivoRut.isEmpty())
            urlRut = storageService.subirDocumentoVerificacion(archivoRut);

        SolicitudVerificacion solicitud = new SolicitudVerificacion();
        solicitud.setRepresentanteLegal(representante);
        solicitud.setRazonSocial(request.getRazonSocial());
        solicitud.setNit(request.getNit());
        solicitud.setCorreoEmpresarial(request.getCorreoEmpresarial());
        solicitud.setUrlRut(urlRut);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaSolicitud(LocalDateTime.now());

        solicitudRepository.save(solicitud);
    }

    @Transactional(readOnly = true)
    public SolicitudVerificacionDTO miSolicitud() {
        Usuario representante = authHelper.usuarioAutenticado();
        return solicitudRepository.findFirstByRepresentanteId(representante.getId())
                .map(this::toDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<SolicitudVerificacionDTO> obtenerSolicitudesPendientes(Pageable pageable) {
        return solicitudRepository.findByEstado(EstadoSolicitud.PENDIENTE, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public SolicitudVerificacionDTO obtenerSolicitud(Long solicitudId) {
        return solicitudRepository.findById(solicitudId)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException("Solicitud no encontrada"));
    }

    @Transactional
    public void aprobarSolicitud(Long solicitudId) {
        SolicitudVerificacion solicitud = obtenerPendiente(solicitudId);
        Usuario representante = solicitud.getRepresentanteLegal();

        if (representante.getOrganizacion() != null)
            throw new BusinessException("El usuario ya pertenece a una organización");

        // 1. Crear el perfil de Organizacion con los datos legales + correo de contacto
        Organizacion organizacion = new Organizacion();
        organizacion.setRepresentante(solicitud.getRepresentanteLegal());
        organizacion.setRazonSocial(solicitud.getRazonSocial());
        organizacion.setNit(solicitud.getNit());
        organizacion.setCorreoContacto(solicitud.getCorreoEmpresarial());
        organizacion.setUrlRut(solicitud.getUrlRut());
        organizacionRepository.save(organizacion);

        // 2. El usuario mantiene correo y clave; solo se le asigna la organización y el rol
        representante.setOrganizacion(organizacion);
        representante.setRol(rolesRepository.findByNombre("REPRESENTANTE")
                .orElseThrow(() -> new BusinessException("Rol REPRESENTANTE no configurado")));
        usuarioRepository.save(representante);

        // 3. Cerrar la solicitud
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud.setAdministradorQueResolvi(authHelper.usuarioAutenticado());
        solicitudRepository.save(solicitud);
    }

    @Transactional
    public void rechazarSolicitud(Long solicitudId, String motivo) {
        SolicitudVerificacion solicitud = obtenerPendiente(solicitudId);
        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud.setAdministradorQueResolvi(authHelper.usuarioAutenticado());
        solicitud.setMotivoRechazo(motivo);
        solicitudRepository.save(solicitud);
    }

    private SolicitudVerificacion obtenerPendiente(Long id) {
        SolicitudVerificacion s = solicitudRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Solicitud no encontrada"));
        if (s.getEstado() != EstadoSolicitud.PENDIENTE)
            throw new BusinessException("Solo se pueden gestionar solicitudes en estado PENDIENTE");
        return s;
    }

    private SolicitudVerificacionDTO toDTO(SolicitudVerificacion s) {
        SolicitudVerificacionDTO dto = new SolicitudVerificacionDTO();
        dto.setId(s.getId());
        dto.setEstado(s.getEstado());
        dto.setMensaje(s.getMensaje());
        dto.setRazonSocial(s.getRazonSocial());
        dto.setNit(s.getNit());
        dto.setRepresentanteLegal(s.getRepresentanteLegal().getId());
        dto.setUrlRut(s.getUrlRut());
        dto.setFechaSolicitud(s.getFechaSolicitud());
        dto.setFechaResolucion(s.getFechaResolucion());
        dto.setMotivoRechazo(s.getMotivoRechazo());
        if (s.getRepresentanteLegal() != null) {
            dto.setRepresentanteId(s.getRepresentanteLegal().getId());
            dto.setRepresentanteNombre(s.getRepresentanteLegal().getNombreCompleto());
            dto.setRepresentanteCorreo(s.getRepresentanteLegal().getCorreo());
        }
        if (s.getAdministradorQueResolvi() != null)
            dto.setAdministradorNombre(s.getAdministradorQueResolvi().getNombreCompleto());
        return dto;
    }

    public void solicitarCorreccion(Long solicitudId, String motivo) {
        SolicitudVerificacion solicitud = obtenerPendiente(solicitudId);
        solicitud.setEstado(EstadoSolicitud.CORRECCION_SOLICITADA);
        solicitud.setMotivoRechazo(motivo);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud.setAdministradorQueResolvi(authHelper.usuarioAutenticado());
        solicitudRepository.save(solicitud);
    }
}