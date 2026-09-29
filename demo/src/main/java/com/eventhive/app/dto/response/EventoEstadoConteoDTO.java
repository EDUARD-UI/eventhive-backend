package com.eventhive.app.dto.response;

import com.eventhive.app.enums.EstadoEvento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoEstadoConteoDTO {
    private EstadoEvento estado;
    private long cantidad;
}
