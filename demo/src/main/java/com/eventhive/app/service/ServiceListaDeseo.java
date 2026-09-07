package com.eventhive.app.service;

import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.response.EventoDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.ListaDeseo;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.ListaDeseoRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceListaDeseo {

    private final ListaDeseoRepository listaDeseoRepository;
    private final ServiceEvento serviceEvento;
    private final AuthenticatedUserHelper authHelper;

    //OPERACIONES
    @Transactional
    public void agregar(Long eventoId) {
        Usuario usuario = authHelper.usuarioAutenticado();
        Evento evento = serviceEvento.obtenerEventoPublicoPorId(eventoId);

        if (listaDeseoRepository.existsByUsuarioIdAndEventoId(usuario.getId(), eventoId)) {
            throw new BusinessException("El evento ya está en tu lista de deseados");
        }

        ListaDeseo deseo = new ListaDeseo();
        deseo.setUsuario(usuario);
        deseo.setEvento(evento);
        listaDeseoRepository.save(deseo);
    }

    @Transactional
    public void quitar(Long eventoId) {
        Usuario usuario = authHelper.usuarioAutenticado();

        if (!listaDeseoRepository.existsByUsuarioIdAndEventoId(usuario.getId(), eventoId)) {
            throw new ResourceNotFoundException("El evento no está en tu lista de deseados");
        }

        listaDeseoRepository.deleteByUsuarioIdAndEventoId(usuario.getId(), eventoId);
    }

    //CONSULTAS
    @Transactional(readOnly = true)
    public PagedResponse<EventoDTO> listarMisDeseados(Pageable pageable) {
        Usuario usuario = authHelper.usuarioAutenticado();
        Page<Evento> page = listaDeseoRepository.findEventosDeseadosByUsuarioId(usuario.getId(), pageable);
        return serviceEvento.toPagedDTO(page);
    }
}
