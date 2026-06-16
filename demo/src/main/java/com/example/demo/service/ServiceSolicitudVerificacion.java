package com.example.demo.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.SolicitudVerificacionDTO;
import com.example.demo.dto.SolicitudVerificacionRequest;
import com.example.demo.enums.EstadoSolicitud;
import com.example.demo.exception.BusinessException;
import com.example.demo.model.Rol;
import com.example.demo.model.SolicitudVerificacion;
import com.example.demo.model.Usuario;
import com.example.demo.repository.RolesRepository;
import com.example.demo.repository.SolicitudVerificacionRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.utils.AuthenticatedUserHelper;
import com.example.demo.utils.PasswordGeneratorUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceSolicitudVerificacion {

    private final SolicitudVerificacionRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolesRepository rolesRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatedUserHelper authHelper;
    private final SupabaseStorageService storageService;

    // Crea una nueva solicitud de verificación con datos empresariales y sube el RUT a Supabase
    //validando que el correo empresarial no esté en uso
    @Transactional
    public void crearSolicitud(SolicitudVerificacionRequest request, MultipartFile archivoRut) {
        Usuario organizador = authHelper.usuarioAutenticado();

        if (solicitudRepository.existsByOrganizadorIdAndEstado(organizador.getId(), EstadoSolicitud.PENDIENTE)) {
            throw new BusinessException("Ya tiene una solicitud pendiente de revisión");
        }

        // El correo empresarial no puede estar registrado como usuario
        if (usuarioRepository.existsByCorreo(request.getCorreoEmpresarial())) {
            throw new BusinessException("El correo empresarial ya está registrado en el sistema");
        }

        // Tampoco puede estar reservado en otra solicitud pendiente o aprobada
        if (solicitudRepository.existsByCorreoEmpresarial(request.getCorreoEmpresarial())) {
            throw new BusinessException("El correo empresarial ya está asociado a otra solicitud");
        }

        String urlRut = null;
        if (archivoRut != null && !archivoRut.isEmpty()) {
            urlRut = storageService.subirArchivo(archivoRut);
        }

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

    // Retorna la solicitud más reciente del organizador autenticado
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public SolicitudVerificacionDTO miSolicitud() {
        Usuario organizador = authHelper.usuarioAutenticado();
        return solicitudRepository.findFirstByOrganizadorId(organizador.getId())
                .map(this::toDTO)
                .orElse(null);
    }

    // Retorna todas las solicitudes PENDIENTES paginadas para el panel del administrador
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Page<SolicitudVerificacionDTO> obtenerSolicitudesPendientes(Pageable pageable) {
        return solicitudRepository.findByEstado(EstadoSolicitud.PENDIENTE, pageable).map(this::toDTO);
    }

    // Obtiene el detalle completo de una solicitud por su ID
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public SolicitudVerificacionDTO obtenerSolicitud(String solicitudId) {
        return solicitudRepository.findById(solicitudId)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException("Solicitud no encontrada"));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String aprobarSolicitud(String solicitudId) {
        SolicitudVerificacion solicitud = obtenerPendiente(solicitudId);

        Rol rolOrganizador = rolesRepository.findByNombre("ORGANIZADOR")
                .orElseThrow(() -> new BusinessException("Rol ORGANIZADOR no encontrado"));

        String claveGenerada = PasswordGeneratorUtil.generar(12);

        // Crea el nuevo usuario con el correo empresarial y la clave generada
        Usuario nuevoOrganizador = new Usuario();
        nuevoOrganizador.setNombre(solicitud.getRazonSocial());
        nuevoOrganizador.setApellido("");
        nuevoOrganizador.setCorreo(solicitud.getCorreoEmpresarial());
        nuevoOrganizador.setClave(passwordEncoder.encode(claveGenerada));
        nuevoOrganizador.setRol(rolOrganizador);
        nuevoOrganizador.setEsVerificado(true);
        usuarioRepository.save(nuevoOrganizador);

        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud.setAdministradorQueResolvi(authHelper.usuarioAutenticado());
        solicitudRepository.save(solicitud);

        // Retorna la clave generada para que el admin la comunique al organizador
        return claveGenerada;
    }

    // Rechaza la solicitud registrando el motivo del rechazo
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void rechazarSolicitud(String solicitudId, String motivo) {
        SolicitudVerificacion solicitud = obtenerPendiente(solicitudId);

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud.setAdministradorQueResolvi(authHelper.usuarioAutenticado());
        solicitud.setMotivoRechazo(motivo);
        solicitudRepository.save(solicitud);
    }

    // --- helpers privados ---
    // Recupera la solicitud y valida que esté en estado PENDIENTE
    private SolicitudVerificacion obtenerPendiente(String id) {
        SolicitudVerificacion s = solicitudRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Solicitud no encontrada"));
        if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new BusinessException("Solo se pueden gestionar solicitudes en estado PENDIENTE");
        }
        return s;
    }

    // Mapea la entidad SolicitudVerificacion a su DTO de respuesta
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
            dto.setOrganizadorNombre(s.getOrganizador().getNombre() + " " + s.getOrganizador().getApellido());
            dto.setOrganizadorCorreo(s.getOrganizador().getCorreo());
        }
        if (s.getAdministradorQueResolvi() != null) {
            dto.setAdministradorNombre(s.getAdministradorQueResolvi().getNombre());
        }
        return dto;
    }
}
