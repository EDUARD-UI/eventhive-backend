package com.eventhive.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatbotEventoDTO {

    private Long id;
    private String title;
    private String nombre;
    private String fecha;
    private String date;
    private String lugar;
    private String categoria;
    private String url;
}
