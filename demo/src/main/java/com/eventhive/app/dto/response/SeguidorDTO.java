package com.eventhive.app.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class SeguidorDTO {

    private Long id;
    private String nombre;
    private LocalDateTime fechaSeguimiento;
}
