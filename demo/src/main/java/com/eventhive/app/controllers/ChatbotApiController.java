package com.eventhive.app.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.request.ChatbotPreguntaRequest;
import com.eventhive.app.dto.response.ChatbotRespuestaDTO;
import com.eventhive.app.service.ServiceChatbot;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotApiController {

    private final ServiceChatbot serviceChatbot;

    @PostMapping("/mensaje")
    public ResponseEntity<ApiResponse<ChatbotRespuestaDTO>> enviarMensaje(@Valid @RequestBody ChatbotPreguntaRequest request) {
        ChatbotRespuestaDTO respuesta = serviceChatbot.procesarMensaje(request.getMensaje());
        return ResponseEntity.ok(ApiResponse.ok("Respuesta obtenida con éxito", respuesta));
    }
}
