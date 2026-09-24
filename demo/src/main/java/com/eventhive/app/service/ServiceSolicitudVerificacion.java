package com.eventhive.app.service;

import java.time.LocalDateTime;

import com.eventhive.app.model.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.eventhive.app.dto.request.SolicitudVerificacionRequest;
import com.eventhive.app.dto.response.SolicitudVerificacionDTO;
import com.eventhive.app.enums.EstadoOrganizacion;
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

@Service
@RequiredArgsConstructor
public class ServiceSolicitudVerificacion {

    private final SolicitudVerificacionRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacionRepository organizacionRepository;
    private final RolesRepository rolesRepository;
    private final AuthenticatedUserHelper authHelper;
    private final SupabaseStorageService storageService;
    private final ServiceNotification serviceNotification;
    private final PasswordEncoder passwordEncoder;

    //CONSULTAS
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

    //OPERACIONES DE CREACION Y ACTUALIZACION DE SOLICITUDES
    // Crea cuenta + organización en PRE_REGISTRO.
    @Transactional
    public void registrarOrganizador(SolicitudVerificacionRequest request) {

        if (usuarioRepository.existsByCorreo(request.getCorreoUsuario())) {
            throw new BusinessException("El correo ya está registrado");
        }
        validarDatosUnicos(request);

        Rol rol = rolesRepository.findByNombre("REPRESENTANTE")
                .orElseThrow(() -> new BusinessException("Rol REPRESENTANTE no configurado"));

        Usuario representante = new Usuario();
        representante.setNombreCompleto(request.getNombreCompleto());
        representante.setCorreo(request.getCorreoUsuario());
        representante.setClave(passwordEncoder.encode(request.getPassword()));
        representante.setRol(rol);
        usuarioRepository.save(representante);

        Organizacion organizacion = new Organizacion();
        organizacion.setRepresentante(representante);
        organizacion.setRazonSocial(request.getRazonSocial());
        organizacion.setNit(request.getNit());
        organizacion.setCorreoContacto(request.getCorreoEmpresarial());
        organizacion.setEstado(EstadoOrganizacion.PRE_REGISTRO);
        organizacionRepository.save(organizacion);

        representante.setOrganizacion(organizacion);
        usuarioRepository.save(representante);

        // Solicitud diferida: entra a cola solo cuando suba el RUT
        SolicitudVerificacion solicitud = new SolicitudVerificacion();
        solicitud.setRepresentanteLegal(representante);
        solicitud.setRazonSocial(request.getRazonSocial());
        solicitud.setNit(request.getNit());
        solicitud.setCorreoEmpresarial(request.getCorreoEmpresarial());
        solicitud.setEstado(EstadoSolicitud.INCOMPLETA);
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitudRepository.save(solicitud);
    }


    // la organizacion ya esta creada y se sube el RUT desde el dashboard.
    @Transactional
    public void subirRutDiferido(Long solicitudId, MultipartFile archivoRut) {
        Usuario representante = authHelper.usuarioAutenticado();

        SolicitudVerificacion solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new BusinessException("Solicitud no encontrada"));

        if (!solicitud.getRepresentanteLegal().getId().equals(representante.getId())) {
            throw new BusinessException("No estás autorizado para modificar esta solicitud");
        }

        if (solicitud.getEstado() == EstadoSolicitud.PENDIENTE
                || solicitud.getEstado() == EstadoSolicitud.APROBADA) {
            throw new BusinessException("La solicitud ya está en revisión o aprobada");
        }

        if (archivoRut == null || archivoRut.isEmpty()) {
            throw new BusinessException("El archivo RUT es obligatorio");
        }

        String urlRut = storageService.subirDocumentoVerificacion(archivoRut);
        solicitud.setUrlRut(urlRut);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitudRepository.save(solicitud);
    }

    // GESTIÓN DE MODERACION
    @Transactional
    public void aprobarSolicitud(Long solicitudId) {
        SolicitudVerificacion solicitud = obtenerPendiente(solicitudId);
        Usuario representante = solicitud.getRepresentanteLegal();

        Organizacion organizacion = representante.getOrganizacion();
        if (organizacion == null) {
            throw new BusinessException("El usuario no tiene organización asociada");
        }

        organizacion.setEstado(EstadoOrganizacion.VERIFICADA);
        organizacion.setUrlRut(solicitud.getUrlRut());
        organizacionRepository.save(organizacion);

        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud.setAdministradorQueResolvi(authHelper.usuarioAutenticado());
        solicitudRepository.save(solicitud);

        serviceNotification.notificarSolicitudOrganizacion(representante, true, null);
    }

    @Transactional
    public void rechazarSolicitud(Long solicitudId, String motivo) {
        SolicitudVerificacion solicitud = obtenerPendiente(solicitudId);
        Usuario representante = solicitud.getRepresentanteLegal();

        if (representante.getOrganizacion() != null) {
            representante.getOrganizacion().setEstado(EstadoOrganizacion.RECHAZADA);
            organizacionRepository.save(representante.getOrganizacion());
        }

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud.setAdministradorQueResolvi(authHelper.usuarioAutenticado());
        solicitud.setMotivoRechazo(motivo);
        solicitudRepository.save(solicitud);

        serviceNotification.notificarSolicitudOrganizacion(representante, false, motivo);
    }

    @Transactional
    public void solicitarCorreccion(Long solicitudId, String motivo) {
        SolicitudVerificacion solicitud = obtenerPendiente(solicitudId);
        solicitud.setEstado(EstadoSolicitud.CORRECCION_SOLICITADA);
        solicitud.setMotivoRechazo(motivo);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud.setAdministradorQueResolvi(authHelper.usuarioAutenticado());
        solicitudRepository.save(solicitud);
    }

    @Transactional
    public void reenviarSolicitud(Long solicitudId, SolicitudVerificacionRequest request, MultipartFile archivoRut) {
        Usuario representante = authHelper.usuarioAutenticado();
        SolicitudVerificacion solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new BusinessException("Solicitud no encontrada"));

        if (!solicitud.getRepresentanteLegal().getId().equals(representante.getId())) {
            throw new BusinessException("No autorizado para modificar esta solicitud");
        }
        if (solicitud.getEstado() != EstadoSolicitud.CORRECCION_SOLICITADA
                && solicitud.getEstado() != EstadoSolicitud.INCOMPLETA) {
            throw new BusinessException("La solicitud no está en estado reenviable");
        }

        solicitud.setRazonSocial(request.getRazonSocial());
        solicitud.setNit(request.getNit());
        solicitud.setCorreoEmpresarial(request.getCorreoEmpresarial());

        if (archivoRut != null && !archivoRut.isEmpty()) {
            String urlRut = storageService.subirDocumentoVerificacion(archivoRut);
            solicitud.setUrlRut(urlRut);
        }

        if (solicitud.getUrlRut() == null) {
            throw new BusinessException("Debes adjuntar el RUT para reenviar");
        }

        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitudRepository.save(solicitud);

        if (representante.getOrganizacion() != null) {
            Organizacion org = representante.getOrganizacion();
            org.setRazonSocial(request.getRazonSocial());
            org.setNit(request.getNit());
            org.setCorreoContacto(request.getCorreoEmpresarial());
            organizacionRepository.save(org);
        }
    }

    // METODOS AUXILIARES Y MAPEO
    private void validarDatosUnicos(SolicitudVerificacionRequest request) {
        if (organizacionRepository.existsByNit(request.getNit())) {
            throw new BusinessException("Ese NIT ya está registrado");
        }
        if (organizacionRepository.existsByCorreoContacto(request.getCorreoEmpresarial())) {
            throw new BusinessException("Ese correo empresarial ya está en uso");
        }
    }

    private SolicitudVerificacion obtenerPendiente(Long id) {
        SolicitudVerificacion s = solicitudRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Solicitud no encontrada"));
        if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new BusinessException("Solo se pueden gestionar solicitudes en estado PENDIENTE");
        }
        return s;
    }

    private SolicitudVerificacionDTO toDTO(SolicitudVerificacion s) {
        SolicitudVerificacionDTO dto = new SolicitudVerificacionDTO();
        dto.setId(s.getId());
        dto.setEstado(s.getEstado());
        dto.setMensaje(s.getMensaje());
        dto.setRazonSocial(s.getRazonSocial());
        dto.setNit(s.getNit());
        dto.setFechaSolicitud(s.getFechaSolicitud());
        dto.setFechaResolucion(s.getFechaResolucion());
        dto.setMotivoRechazo(s.getMotivoRechazo());

        if (s.getRepresentanteLegal() != null) {
            dto.setRepresentanteId(s.getRepresentanteLegal().getId());
            dto.setRepresentanteNombre(s.getRepresentanteLegal().getNombreCompleto());
            dto.setRepresentanteCorreo(s.getRepresentanteLegal().getCorreo());
        }
        if (s.getAdministradorQueResolvi() != null) {
            dto.setAdministradorNombre(s.getAdministradorQueResolvi().getNombreCompleto());
        }
        return dto;
    }
}