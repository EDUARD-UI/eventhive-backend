package com.eventhive.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotPreguntaRequest {

    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(max = 350, message = "El mensaje no puede exceder los 350 caracteres para optimizar el consumo de tokens")
    private String mensaje;
}
