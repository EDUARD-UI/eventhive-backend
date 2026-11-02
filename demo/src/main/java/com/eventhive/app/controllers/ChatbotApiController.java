package com.eventhive.app.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.request.ChatbotPreguntaRequest;
import com.eventhive.app.dto.response.ChatbotRespuestaDTO;
import com.eventhive.app.service.ChatbotRateLimiter;
import com.eventhive.app.service.ServiceChatbot;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping({"/api/chatbot", "/api/chat"})
@RequiredArgsConstructor
public class ChatbotApiController {

    private final ServiceChatbot serviceChatbot;
    private final ChatbotRateLimiter rateLimiter;

    @PostMapping({"/mensaje", "", "/chat"})
    public ResponseEntity<ApiResponse<ChatbotRespuestaDTO>> enviarMensaje(
            @Valid @RequestBody(required = false) ChatbotPreguntaRequest request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest != null ? httpRequest.getHeader("X-Forwarded-For") : null;
        if (ip != null && !ip.isBlank()) {
            int comma = ip.indexOf(',');
            ip = (comma > 0 ? ip.substring(0, comma) : ip).trim();
        }
        if (ip == null || ip.isBlank()) {
            ip = httpRequest != null ? httpRequest.getRemoteAddr() : "127.0.0.1";
        }
        if (!rateLimiter.permitir(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "60")
                    .body(ApiResponse.error("Has superado el límite de 5 solicitudes por minuto. Inténtalo más tarde."));
        }

        String mensaje = (request != null && request.getMensaje() != null) ? request.getMensaje() : "";
        try {
            ChatbotRespuestaDTO respuesta = serviceChatbot.procesarMensaje(mensaje);
            return ResponseEntity.ok(ApiResponse.ok(respuesta.getRespuesta(), respuesta));
        } catch (Exception ex) {
            log.error("Excepción al procesar consulta del chatbot: {}. Activando contingencia.", ex.getMessage(), ex);
            ChatbotRespuestaDTO fallback = ChatbotRespuestaDTO.builder()
                    .respuesta("¡Hola! Soy HiveBot. En este momento estoy actualizando la información de cartelera. Puedes explorar los eventos en la pantalla principal o navegar por categorías.")
                    .fuente("CONTINGENCIA")
                    .modoContingencia(true)
                    .build();
            return ResponseEntity.ok(ApiResponse.ok(fallback.getRespuesta(), fallback));
        }
    }
}
