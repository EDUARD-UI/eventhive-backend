package com.eventhive.app.service;

import com.eventhive.app.dto.response.PromocionDTO;
import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.enums.EstadoPosicionamiento;
import com.eventhive.app.enums.EstadoPromocion;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.PosicionamientoEvento;
import com.eventhive.app.model.Promocion;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.repository.PosicionamientoEventoRepository;
import com.eventhive.app.repository.PromocionRepository;
import com.eventhive.app.repository.TiqueteRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ServicePromocion {

    private final PromocionRepository promocionRepository;
    private final PosicionamientoEventoRepository posicionamientoEventoRepository;
    private final EventoRepository eventoRepository;
    private final TiqueteRepository tiqueteRepository;
    private final AuthenticatedUserHelper authHelper;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    //CONSULTAS
    @Transactional(readOnly = true)
    public Page<PromocionDTO> obtenerTodasPromociones(Pageable pageable) {
        return promocionRepository.findAllConEvento(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public PromocionDTO obtenerPromocionVigente(Long eventoId) {
        return promocionRepository.findVigenteByEventoId(eventoId, LocalDate.now())
                .map(this::toDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<PromocionDTO> obtenerDTOPorOrganizacion(Long organizacionId, Pageable pageable) {
        return promocionRepository.findByOrganizacionId(organizacionId, pageable).map(this::toDTO);
    }

    //OPERACIONES DE POSICIONAMIENTO SEO
    @Transactional
    public Evento posicionarEvento(Long eventoId) {
        Usuario usuario = authHelper.usuarioAutenticado();

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new BusinessException("Evento no encontrado"));

        validarPropietarioEvento(evento, usuario);

        if (evento.getEstado() != EstadoEvento.PUBLICADO) {
            throw new BusinessException("Solo se pueden posicionar eventos publicados");
        }

        if (Boolean.TRUE.equals(evento.getPromocionado())) {
            throw new BusinessException("El evento ya está posicionado");
        }

        if (posicionamientoEventoRepository.existsByEventoIdAndEstado(eventoId, EstadoPosicionamiento.CONFIRMADO)) {
            throw new BusinessException("El evento ya tiene un posicionamiento contratado");
        }

        PosicionamientoEvento posicionamiento = new PosicionamientoEvento();
        posicionamiento.setEvento(evento);
        posicionamiento.setOrganizador(usuario);
        posicionamiento.setPrecio(new BigDecimal("9.00"));
        posicionamiento.setMetodoPago("TARJETA");
        posicionamiento.setEstado(EstadoPosicionamiento.CONFIRMADO);
        posicionamientoEventoRepository.save(posicionamiento);
        evento.setPromocionado(true);

        return eventoRepository.save(evento);
    }

    @Transactional
    public Evento quitarPosicionamiento(Long eventoId) {
        Usuario usuario = authHelper.usuarioAutenticado();

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new BusinessException("Evento no encontrado"));

        validarPropietarioEvento(evento, usuario);

        if (!Boolean.TRUE.equals(evento.getPromocionado())) {
            throw new BusinessException("El evento no está posicionado");
        }

        posicionamientoEventoRepository.findByEventoIdAndEstado(eventoId, EstadoPosicionamiento.CONFIRMADO)
                .ifPresent(p -> p.setEstado(EstadoPosicionamiento.CANCELADO));

        evento.setPromocionado(false);
        return eventoRepository.save(evento);
    }

    //OPERACIONES CRUD PROMOCIONES
    @Transactional
    public void crearPromocion(Long eventoId, String descripcion, BigDecimal descuento,
                               String fechaInicio, String fechaFin, Usuario usuario) {
        validarDescuento(descuento);
        LocalDate inicio = LocalDate.parse(fechaInicio, FMT);
        LocalDate fin = LocalDate.parse(fechaFin, FMT);
        validarFechas(inicio, fin);

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado: " + eventoId));

        validarEventoParaCrearPromocion(evento);

        if (!esAdmin(usuario) && !esRepresentanteDeLaOrganizacion(evento, usuario))
            throw new BusinessException("No autorizado: el evento no pertenece a tu organización");

        // Un evento no puede tener dos promociones activas en el mismo rango de fechas
        if (promocionRepository.existsConflictoFechas(eventoId, inicio, fin, null))
            throw new BusinessException(
                    "El evento ya tiene una promoción activa en ese rango de fechas");

        Promocion p = new Promocion();
        p.setDescripcion(descripcion);
        p.setDescuento(descuento);
        p.setFechaInicio(inicio);
        p.setFechaFin(fin);
        p.setEvento(evento);
        promocionRepository.save(p);
    }

    @Transactional
    public void actualizarPromocion(Long id, Long eventoId, String descripcion, BigDecimal descuento,
                                    String fechaInicio, String fechaFin, Usuario usuario) {
        Promocion p = obtenerPromocionPorId(id);
        validarPermiso(p, usuario);
        validarEdicionPromocion(p.getEvento());
        validarDescuento(descuento);
        LocalDate inicio = LocalDate.parse(fechaInicio, FMT);
        LocalDate fin = LocalDate.parse(fechaFin, FMT);
        validarFechas(inicio, fin);

        if (eventoId != null) {
            Evento nuevoEvento = eventoRepository.findById(eventoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado: " + eventoId));

            if (!esAdmin(usuario) && !esRepresentanteDeLaOrganizacion(nuevoEvento, usuario))
                throw new BusinessException("No autorizado: el evento no pertenece a tu organización");

            validarEventoParaCrearPromocion(nuevoEvento);

            // Excluir la propia promoción al verificar conflicto (permite guardar sin cambios de fecha)
            if (promocionRepository.existsConflictoFechas(eventoId, inicio, fin, id))
                throw new BusinessException(
                        "El evento ya tiene una promoción activa en ese rango de fechas");

            p.setEvento(nuevoEvento);
        }

        p.setDescripcion(descripcion);
        p.setDescuento(descuento);
        p.setFechaInicio(inicio);
        p.setFechaFin(fin);
        promocionRepository.save(p);
    }

    @Transactional
    public void eliminarPromocion(Long id, Usuario usuario) {
        Promocion p = obtenerPromocionPorId(id);
        validarPermiso(p, usuario);
        validarEdicionPromocion(p.getEvento());
        promocionRepository.deleteById(id);
    }

    //METODOS AUXILIARES Y MAPEO
    private Promocion obtenerPromocionPorId(Long id) {
        return promocionRepository.findByIdConEvento(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada: " + id));
    }

    private void validarPropietarioEvento(Evento evento, Usuario usuario) {

        if (evento.getOrganizacion() == null) {
            throw new BusinessException("El evento no tiene una organización asociada");
        }

        if (evento.getOrganizacion().getRepresentante() == null
                || !evento.getOrganizacion().getRepresentante().getId().equals(usuario.getId())) {

            throw new BusinessException("No tienes permisos para modificar el posicionamiento de este evento");
        }
    }

    private void validarEventoParaCrearPromocion(Evento evento) {
        if (evento.getFecha() == null || evento.getHora() == null) {
            throw new BusinessException("El evento debe tener fecha y hora para gestionar una promoción");
        }

        if (evento.getEstado() == com.eventhive.app.enums.EstadoEvento.FINALIZADO) {
            throw new BusinessException("No se puede crear o mover una promoción a un evento finalizado");
        }

        if (LocalDateTime.of(evento.getFecha(), evento.getHora()).isBefore(LocalDateTime.now())) {
            throw new BusinessException("No se puede crear o mover una promoción cuando el evento ya comenzó");
        }
    }

    private void validarEdicionPromocion(Evento evento) {
        if (evento == null) {
            throw new BusinessException("La promoción debe estar asociada a un evento");
        }

        if (evento.getFecha() != null && evento.getHora() != null
                && !LocalDateTime.of(evento.getFecha(), evento.getHora()).isAfter(LocalDateTime.now())) {
            throw new BusinessException("No se puede modificar o eliminar una promoción cuando el evento ya comenzó");
        }

        if (tiqueteRepository.existsByEventoId(evento.getId())) {
            throw new BusinessException("No se puede modificar o eliminar la promoción porque el evento ya tiene boletos vendidos");
        }
    }

    private void validarDescuento(BigDecimal d) {
        if (d.compareTo(BigDecimal.ONE) < 0 || d.compareTo(new BigDecimal("75")) > 0)
            throw new BusinessException("El descuento debe estar entre 1 y 75");
    }

    private void validarFechas(LocalDate inicio, LocalDate fin) {
        if (fin.isBefore(inicio))
            throw new BusinessException("La fecha de fin no puede ser anterior a la de inicio");
    }

    private boolean esAdmin(Usuario u) {
        return u.getRol() != null && "ADMINISTRADOR".equals(u.getRol().getNombre());
    }

    private void validarPermiso(Promocion p, Usuario usuario) {
        if (esAdmin(usuario)) return;
        boolean autorizado = p.getEvento() != null && esRepresentanteDeLaOrganizacion(p.getEvento(), usuario);
        if (!autorizado)
            throw new BusinessException("No autorizado para modificar esta promoción");
    }

    //solo el representante de la organizacion puede hacer promociones a un evento de la organizacion
    private boolean esRepresentanteDeLaOrganizacion(Evento evento, Usuario usuario) {
        return evento.getOrganizacion() != null
                && evento.getOrganizacion().getRepresentante() != null
                && evento.getOrganizacion().getRepresentante().getId().equals(usuario.getId());
    }

    private EstadoPromocion calcularEstado(Promocion p, LocalDate hoy) {
        if (hoy.isBefore(p.getFechaInicio())) return EstadoPromocion.PROGRAMADA;
        if (hoy.isAfter(p.getFechaFin())) return EstadoPromocion.VENCIDA;
        return EstadoPromocion.VIGENTE;
    }

    //METODO DE MAPEO
    public PromocionDTO toDTO(Promocion p) {
        PromocionDTO dto = new PromocionDTO();
        dto.setId(p.getId());
        dto.setDescripcion(p.getDescripcion());
        dto.setDescuento(p.getDescuento());
        dto.setFechaInicio(p.getFechaInicio());
        dto.setFechaFinal(p.getFechaFin());
        dto.setEstado(calcularEstado(p, LocalDate.now()));

        if (p.getEvento() != null) {
            dto.setEventoId(p.getEvento().getId());
            dto.setEventoTitulo(p.getEvento().getTitulo());
            dto.setEventoNombre(p.getEvento().getTitulo());
        }
        return dto;
    }
}