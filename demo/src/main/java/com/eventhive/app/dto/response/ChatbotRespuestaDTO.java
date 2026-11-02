package com.eventhive.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ChatbotRespuestaDTO {

    private String respuesta;
    private String fuente;
    private boolean modoContingencia;

    @JsonProperty("response")
    public String getResponse() {
        return respuesta;
    }

    @JsonProperty("message")
    public String getMessage() {
        return respuesta;
    }

    @JsonProperty("mensaje")
    public String getMensaje() {
        return respuesta;
    }

    @JsonProperty("texto")
    public String getTexto() {
        return respuesta;
    }

    @JsonProperty("reply")
    public String getReply() {
        return respuesta;
    }

    @JsonProperty("content")
    public String getContent() {
        return respuesta;
    }
}
