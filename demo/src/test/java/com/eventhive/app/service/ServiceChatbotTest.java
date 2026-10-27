package com.eventhive.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventhive.app.dto.response.ChatbotRespuestaDTO;
import com.eventhive.app.model.Categoria;
import com.eventhive.app.repository.CategoriaRepository;
import com.eventhive.app.repository.EventoRepository;

@ExtendWith(MockitoExtension.class)
class ServiceChatbotTest {

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    private ServiceChatbot serviceChatbot;

    @BeforeEach
    void setUp() {
        serviceChatbot = new ServiceChatbot(eventoRepository, categoriaRepository, "https://api.groq.com/openai/v1");
    }

    @Test
    @DisplayName("Debe interceptar consultas no relacionadas con eventos (Guardrail local - 0 tokens)")
    void testGuardrailFueraDeTema() {
        ChatbotRespuestaDTO respuesta = serviceChatbot.procesarMensaje("¿Cómo preparo un café?");
        assertNotNull(respuesta);
        assertEquals("LOCAL_GUARDRAIL", respuesta.getFuente());
        assertFalse(respuesta.isModoContingencia());
        assertTrue(respuesta.getRespuesta().contains("asistente virtual exclusivo de EventHive"));
    }

    @Test
    @DisplayName("Debe responder saludos comunes localmente sin consumir tokens ni llamadas a Groq")
    void testSaludoLocal() {
        ChatbotRespuestaDTO respuesta = serviceChatbot.procesarMensaje("Hola, buenos días");
        assertNotNull(respuesta);
        assertEquals("LOCAL_RULE", respuesta.getFuente());
        assertFalse(respuesta.isModoContingencia());
        assertTrue(respuesta.getRespuesta().contains("EventHive"));
    }

    @Test
    @DisplayName("Debe responder dudas sobre cómo ser organizador localmente sin consumir tokens")
    void testPreguntaOrganizadorLocal() {
        ChatbotRespuestaDTO respuesta = serviceChatbot.procesarMensaje("¿Cómo puedo ser organizador y publicar?");
        assertNotNull(respuesta);
        assertEquals("LOCAL_RULE", respuesta.getFuente());
        assertFalse(respuesta.isModoContingencia());
        assertTrue(respuesta.getRespuesta().contains("Convertirse en Organizador"));
    }

    @Test
    @DisplayName("Debe responder preguntas sobre categorías disponibles localmente")
    void testPreguntaCategorias() {
        Categoria cat1 = new Categoria();
        cat1.setId(1L);
        cat1.setNombre("Conciertos");
        when(categoriaRepository.findAll()).thenReturn(List.of(cat1));

        ChatbotRespuestaDTO respuesta = serviceChatbot.procesarMensaje("¿Qué categorías de eventos tienen?");
        assertNotNull(respuesta);
        assertEquals("LOCAL_RULE", respuesta.getFuente());
        assertTrue(respuesta.getRespuesta().contains("Conciertos"));
    }

    @Test
    @DisplayName("Debe responder preguntas sobre la fecha actual")
    void testPreguntaFecha() {
        ChatbotRespuestaDTO respuesta = serviceChatbot.procesarMensaje("¿Qué fecha es hoy?");
        assertNotNull(respuesta);
        assertEquals("LOCAL_RULE", respuesta.getFuente());
        assertTrue(respuesta.getRespuesta().contains("Hoy es"));
    }

    @Test
    @DisplayName("Debe responder con plan de contingencia si no hay API key de Groq configurada")
    void testContingenciaSinApiKey() {
        ChatbotRespuestaDTO respuesta = serviceChatbot.procesarMensaje("¿Cuáles son los métodos de pago para comprar boletos?");
        assertNotNull(respuesta);
        assertEquals("CONTINGENCIA", respuesta.getFuente());
        assertTrue(respuesta.isModoContingencia());
        assertNotNull(respuesta.getRespuesta());
        assertFalse(respuesta.getRespuesta().isBlank());
    }
}
