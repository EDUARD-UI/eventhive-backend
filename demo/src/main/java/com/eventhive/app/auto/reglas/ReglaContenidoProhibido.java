package com.eventhive.app.auto.reglas;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.eventhive.app.enums.MotivosRechazos;
import com.eventhive.app.enums.SeveridadPalabra;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.PalabraProhibida;
import com.eventhive.app.auto.ResultadoReglaModeracion;
import com.eventhive.app.auto.ReglaModeracion;
import com.eventhive.app.repository.PalabraProhibidaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReglaContenidoProhibido implements ReglaModeracion {

    private final PalabraProhibidaRepository palabraProhibidaRepository;

    @Override
    public ResultadoReglaModeracion evaluar(Evento evento) {
        String texto = normalizar(evento.getTitulo(), evento.getDescripcion());
        List<PalabraProhibida> palabrasActivas = palabraProhibidaRepository.findByActivaTrue();

        boolean tieneGrave = palabrasActivas.stream()
                .anyMatch(p -> p.getSeveridad() == SeveridadPalabra.GRAVE && texto.contains(p.getPalabra()));
        if (tieneGrave) {
            return ResultadoReglaModeracion.rechazo(
                    MotivosRechazos.INCUMPLE_POLITICAS,
                    "El contenido incluye términos prohibidos por las políticas de EventHive.");
        }

        boolean tieneSospechosa = palabrasActivas.stream()
                .anyMatch(p -> p.getSeveridad() == SeveridadPalabra.SOSPECHOSA && texto.contains(p.getPalabra()));
        if (tieneSospechosa) {
            return ResultadoReglaModeracion.revision(
                    MotivosRechazos.INCUMPLE_POLITICAS,
                    "El contenido incluye términos que requieren revisión manual.");
        }

        return ResultadoReglaModeracion.ok();
    }

    private String normalizar(String titulo, String descripcion) {
        String texto = (titulo == null ? "" : titulo) + " " + (descripcion == null ? "" : descripcion);
        return texto.toLowerCase(Locale.ROOT);
    }
}