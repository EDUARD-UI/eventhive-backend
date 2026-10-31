package com.eventhive.app.recommendation.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.eventhive.app.dto.response.EventoDTO;
import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.recommendation.dto.CandidatoRecomendacionDTO;
import com.eventhive.app.recommendation.dto.EvaluacionRecomendacionRequest;
import com.eventhive.app.recommendation.dto.EvaluacionRecomendacionResponse;
import com.eventhive.app.recommendation.dto.EventoRecomendacionDTO;
import com.eventhive.app.recommendation.dto.RecomendacionResultadoDTO;
import com.eventhive.app.recommendation.event.EventoPublicadoEvent;
import com.eventhive.app.recommendation.repository.RecomendacionRepository;
import com.eventhive.app.recommendation.repository.RegistroRecomendacionRepository;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.service.ServiceCorreo;
import com.eventhive.app.service.ServiceEvento;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicioRecomendacion {

    private static final int MAXIMO_CORREOS = 15;
    private static final int MAX_EVENTOS_RECOMENDADOS = 12;

    private final RecomendacionRepository recomendacionRepository;
    private final RegistroRecomendacionRepository registroRepository;
    private final ClientePythonRecomendacion clientePython;
    private final ServiceCorreo serviceCorreo;
    private final EventoRepository eventoRepository;
    private final ServiceEvento serviceEvento;
    private final AuthenticatedUserHelper authHelper;

    @Value("${recommendation.email.enabled:false}")
    private boolean recomendacionesHabilitadas;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alPublicarEvento(EventoPublicadoEvent event) {
        if (!recomendacionesHabilitadas) {
            log.info("Recomendaciones automáticas por correo deshabilitadas por configuración.");
            return;
        }
        if (event == null || event.getEventoId() == null) {
            return;
        }
        procesarEventoPublicado(event.getEventoId());
    }

    public void procesarEventoPublicado(Long eventoId) {
        try {
            EventoRecomendacionDTO evento = recomendacionRepository.obtenerEventoPublicado(eventoId);
            if (evento == null) {
                log.warn("No se encontró el evento publicado con ID {}", eventoId);
                return;
            }

            List<CandidatoRecomendacionDTO> candidatos = recomendacionRepository.obtenerCandidatos(eventoId);
            if (candidatos.isEmpty()) {
                log.info("No hay candidatos de recomendación para el evento {}", eventoId);
                return;
            }

            EvaluacionRecomendacionRequest request = new EvaluacionRecomendacionRequest(evento, candidatos);
            EvaluacionRecomendacionResponse response = clientePython.evaluar(request);

            if (response == null || response.getRecomendaciones() == null) {
                log.warn("Python no devolvió recomendaciones para el evento {}", eventoId);
                return;
            }

            Map<Long, CandidatoRecomendacionDTO> candidatosPorId = new HashMap<>();
            for (CandidatoRecomendacionDTO candidato : candidatos) {
                candidatosPorId.put(candidato.getUsuarioId(), candidato);
            }

            List<RecomendacionResultadoDTO> recomendados = response.getRecomendaciones().stream()
                    .filter(r -> r.getAfinidad() != null)
                    .filter(r -> "ALTA".equalsIgnoreCase(r.getAfinidad()))
                    .filter(r -> r.getUsuarioId() != null)
                    .filter(r -> candidatosPorId.containsKey(r.getUsuarioId()))
                    .sorted((a, b) -> Integer.compare(
                            b.getPuntuacion() != null ? b.getPuntuacion() : 0,
                            a.getPuntuacion() != null ? a.getPuntuacion() : 0))
                    .limit(MAXIMO_CORREOS)
                    .toList();

            int enviados = 0;
            for (RecomendacionResultadoDTO resultado : recomendados) {
                CandidatoRecomendacionDTO candidato = candidatosPorId.get(resultado.getUsuarioId());
                if (candidato.getCorreo() == null || candidato.getCorreo().isBlank()) {
                    continue;
                }

                boolean reclamado = registroRepository.reclamarEnvio(eventoId, candidato.getUsuarioId());
                if (!reclamado) {
                    continue;
                }

                try {
                    serviceCorreo.enviarRecomendacionEvento(
                            candidato.getNombreCompleto(),
                            candidato.getCorreo(),
                            evento);
                    registroRepository.marcarEnviado(eventoId, candidato.getUsuarioId());
                    enviados++;
                } catch (Exception ex) {
                    registroRepository.marcarError(eventoId, candidato.getUsuarioId());
                    log.error("Error enviando recomendación del evento {} al usuario {}: {}",
                            eventoId, candidato.getUsuarioId(), ex.getMessage());
                }
            }

            log.info("Recomendaciones evento {}: candidatos={}, afinidad alta={}, enviados={}",
                    eventoId, candidatos.size(), recomendados.size(), enviados);

        } catch (Exception ex) {
            log.error("Error procesando recomendaciones del evento {}", eventoId, ex);
        }
    }

    @Transactional(readOnly = true)
    public List<EventoDTO> obtenerRecomendacionesParaUsuarioAutenticado() {
        Usuario usuario = authHelper.usuarioAutenticado();
        return obtenerRecomendaciones(usuario.getId());
    }

    @Transactional(readOnly = true)
    public List<EventoDTO> obtenerRecomendaciones(Long usuarioId) {
        List<Evento> publicados = eventoRepository.findPublicadosVisibles(PageRequest.of(0, 25)).getContent();
        if (publicados.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventoConAfinidad> evaluados = new ArrayList<>();

        for (Evento e : publicados) {
            if (e.getEstado() != EstadoEvento.PUBLICADO) {
                continue;
            }
            CandidatoRecomendacionDTO candidato = recomendacionRepository
                    .obtenerMetricasUsuarioParaEvento(usuarioId, e.getId());

            EventoRecomendacionDTO evDTO = new EventoRecomendacionDTO(
                    e.getId(),
                    e.getTitulo(),
                    e.getDescripcion(),
                    e.getCategoria() != null ? e.getCategoria().getId() : null,
                    e.getCategoria() != null ? e.getCategoria().getNombre() : null,
                    e.getLugar(),
                    e.getFecha()
            );

            EvaluacionRecomendacionRequest request = new EvaluacionRecomendacionRequest(
                    evDTO, List.of(candidato));

            EvaluacionRecomendacionResponse resp = clientePython.evaluar(request);
            int score = 0;
            String afinidad = "BAJA";

            if (resp != null && resp.getRecomendaciones() != null && !resp.getRecomendaciones().isEmpty()) {
                RecomendacionResultadoDTO r = resp.getRecomendaciones().getFirst();
                score = r.getPuntuacion() != null ? r.getPuntuacion() : 0;
                afinidad = r.getAfinidad() != null ? r.getAfinidad() : "BAJA";
            } else {
                long fav = candidato.getFavoritosCategoria() != null ? candidato.getFavoritosCategoria() : 0L;
                long comp = candidato.getComprasCategoria() != null ? candidato.getComprasCategoria() : 0L;
                score = (int) Math.min(100, (fav * 15) + (comp * 25));
                afinidad = score >= 70 ? "ALTA" : (score >= 35 ? "MEDIA" : "BAJA");
            }

            evaluados.add(new EventoConAfinidad(e, score, afinidad));
        }

        evaluados.sort((a, b) -> Integer.compare(b.score, a.score));

        return evaluados.stream()
                .limit(MAX_EVENTOS_RECOMENDADOS)
                .map(ea -> serviceEvento.toDTO(ea.evento))
                .toList();
    }

    private static class EventoConAfinidad {
        final Evento evento;
        final int score;
        final String afinidad;

        EventoConAfinidad(Evento evento, int score, String afinidad) {
            this.evento = evento;
            this.score = score;
            this.afinidad = afinidad;
        }
    }
}

