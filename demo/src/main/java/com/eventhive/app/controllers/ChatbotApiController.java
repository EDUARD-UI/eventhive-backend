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

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotApiController {

    private final ServiceChatbot serviceChatbot;
    private final ChatbotRateLimiter rateLimiter;

    @PostMapping("/mensaje")
    public ResponseEntity<ApiResponse<ChatbotRespuestaDTO>> enviarMensaje(
            @Valid @RequestBody ChatbotPreguntaRequest request,
            HttpServletRequest httpRequest) {
        if (!rateLimiter.permitir(httpRequest.getRemoteAddr())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "60")
                    .body(ApiResponse.error("Has superado el límite de 5 solicitudes por minuto. Inténtalo más tarde."));
        }

        ChatbotRespuestaDTO respuesta = serviceChatbot.procesarMensaje(request.getMensaje());
        return ResponseEntity.ok(ApiResponse.ok("Respuesta obtenida con éxito", respuesta));
    }
}
