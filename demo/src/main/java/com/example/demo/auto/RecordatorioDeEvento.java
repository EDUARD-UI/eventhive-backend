package com.example.demo.auto;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.enums.EstadoEvento;
import com.example.demo.model.Evento;
import com.example.demo.repository.EventoRepository;
import com.example.demo.service.ServiceNotification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordatorioDeEvento {

    private final EventoRepository     eventoRepository;
    private final ServiceNotification  serviceNotification;

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
}