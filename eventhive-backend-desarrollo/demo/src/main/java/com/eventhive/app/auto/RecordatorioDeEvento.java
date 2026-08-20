package com.eventhive.app.auto;

import com.eventhive.app.service.ServiceCorreo;
import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.repository.TiqueteRepository;
import com.eventhive.app.service.ServiceEvento;
import com.eventhive.app.service.ServiceNotification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordatorioDeEvento {

    private final ServiceCorreo serviceCorreo;
    private final TiqueteRepository tiqueteRepository;
    private final EventoRepository eventoRepository;
    private final ServiceNotification serviceNotification;
    private final ServiceEvento serviceEvento;


    // Se ejecuta todos los días a las 9:00 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void enviarRecordatorios() {
        LocalDate manana = LocalDate.now().plusDays(1);

        List<Evento> eventosManana = eventoRepository
                .findByFechaAndEstado(manana, EstadoEvento.PUBLICADO);

        log.info("Enviando recordatorios para {} eventos del {}",
                eventosManana.size(), manana);

        eventosManana.forEach(evento -> {
            try {
                serviceNotification.notificarRecordatorioEvento(evento);
            } catch (Exception ex) {
                log.error("Error al notificar recordatorio del evento {}: {}",
                        evento.getId(), ex.getMessage());
            }
        });
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void enviarRecordatoriosPorCorreoDelDia() {
        LocalDate hoy = LocalDate.now();
        List<Evento> eventosHoy = eventoRepository.findByFechaAndEstado(hoy, EstadoEvento.PUBLICADO);

        eventosHoy.forEach(evento -> {
            List<Usuario> compradores = tiqueteRepository.findClientesByEventoId(evento.getId());
            compradores.forEach(comprador -> {
                try {
                    serviceCorreo.enviarRecordatorioEventoHoy(evento, comprador);
                } catch (Exception ex) {
                    log.error("Error al enviar recordatorio del evento {} a {}: {}",
                            evento.getId(), comprador.getId(), ex.getMessage());
                }
            });
        });
    }

    // Se ejecuta todos los días a la 1:00 AM: cierra eventos publicados cuya fecha
    // ya pasó
    @Scheduled(cron = "0 0 1 * * *")
    public void finalizarEventosVencidos() {
        serviceEvento.finalizarEventosVencidos(LocalDate.now());
    }
}