package com.example.demo.service;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.SolicitudVerificacionDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.model.SolicitudVerificacion;
import com.example.demo.model.Usuario;
import com.example.demo.repository.SolicitudVerificacionRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.utils.AuthenticatedUserHelper;
import com.example.demo.utils.Utilidades;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceSolicitudVerificacion {

    private final SolicitudVerificacionRepository solicitudRepository;
    private final UsuarioRepository               usuarioRepository;
    private final AuthenticatedUserHelper         authHelper;

    @Value("${upload.path.verificacion:uploads/verificacion}")
    private String uploadPath;

    @Transactional
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public void crearSolicitud(String mensaje, MultipartFile archivo) {
        Usuario organizador = authHelper.usuarioAutenticado();

        solicitudRepository.findFirstByOrganizadorId(organizador.getId())
                .filter(s -> "PENDIENTE".equals(s.getEstado()))
                .ifPresent(s -> { throw new BusinessException("Ya tiene una solicitud pendiente"); });

        String nombreArchivo = null;
        if (archivo != null && !archivo.isEmpty()) {
            Utilidades.validarFoto(archivo);
            try { nombreArchivo = Utilidades.guardarFoto(archivo, uploadPath); }
            catch (IOException e) { throw new BusinessException("Error al guardar el archivo: " + e.getMessage()); }
        }

        SolicitudVerificacion s = new SolicitudVerificacion();
        s.setOrganizador(organizador);
        s.setMensaje(mensaje);
        s.setArchivoConfirmacion(nombreArchivo);
        s.setEstado("PENDIENTE");
        s.setFechaSolicitud(LocalDateTime.now());
        solicitudRepository.save(s);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public SolicitudVerificacionDTO miSolicitud() {
        Usuario organizador = authHelper.usuarioAutenticado();
        return solicitudRepository.findFirstByOrganizadorId(organizador.getId())
                .map(this::toDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Page<SolicitudVerificacionDTO> obtenerSolicitudesPendientes(Pageable pageable) {
        return solicitudRepository.findByEstado("PENDIENTE", pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public SolicitudVerificacionDTO obtenerSolicitud(String solicitudId) {
        return solicitudRepository.findById(solicitudId)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException("Solicitud no encontrada"));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void aprobarSolicitud(String solicitudId) {
        SolicitudVerificacion s = obtenerPendiente(solicitudId);

        s.getOrganizador().setEsVerificado(true);
        usuarioRepository.save(s.getOrganizador());

        s.setEstado("APROBADA");
        s.setFechaResolucion(LocalDateTime.now());
        s.setAdministradorQueResolvi(authHelper.usuarioAutenticado());
        solicitudRepository.save(s);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void rechazarSolicitud(String solicitudId, String motivo) {
        SolicitudVerificacion s = obtenerPendiente(solicitudId);

        s.setEstado("RECHAZADA");
        s.setFechaResolucion(LocalDateTime.now());
        s.setAdministradorQueResolvi(authHelper.usuarioAutenticado());
        s.setMotivoRechazo(motivo);
        solicitudRepository.save(s);
    }

    // --- helpers ---

    private SolicitudVerificacion obtenerPendiente(String id) {
        SolicitudVerificacion s = solicitudRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Solicitud no encontrada"));
        if (!"PENDIENTE".equals(s.getEstado()))
            throw new BusinessException("Solo se pueden gestionar solicitudes pendientes");
        return s;
    }

    private SolicitudVerificacionDTO toDTO(SolicitudVerificacion s) {
        SolicitudVerificacionDTO dto = new SolicitudVerificacionDTO();
        dto.setId(s.getId());
        dto.setEstado(s.getEstado());
        dto.setMensaje(s.getMensaje());
        dto.setArchivoConfirmacion(s.getArchivoConfirmacion());
        dto.setFechaSolicitud(s.getFechaSolicitud());
        dto.setFechaResolucion(s.getFechaResolucion());
        dto.setMotivoRechazo(s.getMotivoRechazo());
        if (s.getOrganizador() != null) {
            dto.setOrganizadorId(s.getOrganizador().getId());
            dto.setOrganizadorNombre(s.getOrganizador().getNombre() + " " + s.getOrganizador().getApellido());
            dto.setOrganizadorCorreo(s.getOrganizador().getCorreo());
        }
        if (s.getAdministradorQueResolvi() != null)
            dto.setAdministradorNombre(s.getAdministradorQueResolvi().getNombre());
        return dto;
    }
}