package com.eventhive.app.service;

import com.eventhive.app.dto.response.SolicitudVerificacionDTO;
import com.eventhive.app.dto.request.SolicitudVerificacionRequest;
import com.eventhive.app.enums.EstadoSolicitud;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.SolicitudVerificacion;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.OrganizacionRepository;
import com.eventhive.app.repository.SolicitudVerificacionRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import com.eventhive.app.utils.PasswordGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatedUserHelper authHelper;
    private final SupabaseStorageService storageService;

    @Transactional
    public void crearSolicitud(SolicitudVerificacionRequest request, MultipartFile archivoRut) {
        Usuario organizador = authHelper.usuarioAutenticado();

        if (organizador.getOrganizacion() != null)
            throw new BusinessException("Esta cuenta ya fue verificada como organización");

        if (solicitudRepository.existsByOrganizadorIdAndEstado(organizador.getId(), EstadoSolicitud.PENDIENTE))
            throw new BusinessException("Ya tiene una solicitud pendiente de revisión");

        if (usuarioRepository.existsByCorreo(request.getCorreoEmpresarial()))
            throw new BusinessException("El correo empresarial ya está registrado en el sistema");

        if (solicitudRepository.existsByCorreoEmpresarial(request.getCorreoEmpresarial()))
            throw new BusinessException("El correo empresarial ya está asociado a otra solicitud");

        String urlRut = null;
        if (archivoRut != null && !archivoRut.isEmpty())
            urlRut = storageService.subirDocumentoVerificacion(archivoRut);

        SolicitudVerificacion solicitud = new SolicitudVerificacion();
        solicitud.setOrganizador(organizador);
        solicitud.setRazonSocial(request.getRazonSocial());
        solicitud.setNit(request.getNit());
        solicitud.setRepresentanteLegal(request.getRepresentanteLegal());
        solicitud.setCorreoEmpresarial(request.getCorreoEmpresarial());
        solicitud.setMensaje(request.getMensaje());
        solicitud.setUrlRut(urlRut);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaSolicitud(LocalDateTime.now());

        solicitudRepository.save(solicitud);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ORGANIZACION')")
    public SolicitudVerificacionDTO miSolicitud() {
        Usuario organizador = authHelper.usuarioAutenticado();
        return solicitudRepository.findFirstByOrganizadorId(organizador.getId())
                .map(this::toDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Page<SolicitudVerificacionDTO> obtenerSolicitudesPendientes(Pageable pageable) {
        return solicitudRepository.findByEstado(EstadoSolicitud.PENDIENTE, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public SolicitudVerificacionDTO obtenerSolicitud(Long solicitudId) {
        return solicitudRepository.findById(solicitudId)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException("Solicitud no encontrada"));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String aprobarSolicitud(Long solicitudId) {
        SolicitudVerificacion solicitud = obtenerPendiente(solicitudId);
        Usuario organizador = solicitud.getOrganizador();

        if (organizador.getOrganizacion() != null)
            throw new BusinessException("El organizador ya cuenta con un perfil de organización");

        // El correo empresarial pasa a ser el correo de inicio de sesión; debe seguir libre
        if (!organizador.getCorreo().equalsIgnoreCase(solicitud.getCorreoEmpresarial())
                && usuarioRepository.existsByCorreo(solicitud.getCorreoEmpresarial()))
            throw new BusinessException("El correo empresarial ya está registrado en el sistema");

        // 1. Crear el perfil de Organizacion con los datos legales de la solicitud
        Organizacion organizacion = new Organizacion();
        organizacion.setRazonSocial(solicitud.getRazonSocial());
        organizacion.setNit(solicitud.getNit());
        organizacion.setRepresentanteLegal(solicitud.getRepresentanteLegal());
        organizacion.setUrlRut(solicitud.getUrlRut());
        organizacionRepository.save(organizacion);

        // 2. Completar la cuenta del organizador ya existente (no se crea un usuario nuevo)
        String claveGenerada = PasswordGeneratorUtil.generar(12);
        organizador.setOrganizacion(organizacion);
        organizador.setCorreo(solicitud.getCorreoEmpresarial());
        organizador.setClave(passwordEncoder.encode(claveGenerada));
        usuarioRepository.save(organizador);

        // 3. Cerrar la solicitud
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud.setAdministradorQueResolvi(authHelper.usuarioAutenticado());
        solicitudRepository.save(solicitud);

        return claveGenerada;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
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
        dto.setRepresentanteLegal(s.getRepresentanteLegal());
        dto.setUrlRut(s.getUrlRut());
        dto.setFechaSolicitud(s.getFechaSolicitud());
        dto.setFechaResolucion(s.getFechaResolucion());
        dto.setMotivoRechazo(s.getMotivoRechazo());
        if (s.getOrganizador() != null) {
            dto.setOrganizadorId(s.getOrganizador().getId());
            dto.setOrganizadorNombre(s.getOrganizador().getNombreCompleto());
            dto.setOrganizadorCorreo(s.getOrganizador().getCorreo());
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