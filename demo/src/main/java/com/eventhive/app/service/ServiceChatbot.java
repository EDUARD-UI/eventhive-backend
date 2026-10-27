package com.eventhive.app.service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.eventhive.app.dto.response.ChatbotRespuestaDTO;
import com.eventhive.app.model.Evento;
import com.eventhive.app.repository.EventoRepository;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class ServiceChatbot {

    private static final Logger log = LoggerFactory.getLogger(ServiceChatbot.class);

    private final EventoRepository eventoRepository;
    private final RestClient restClient;

    @Value("${groq.api.key:${spring.ai.openai.api-key:}}")
    private String groqApiKey;

    @Value("${groq.api.model:${spring.ai.openai.chat.options.model:qwen/qwen3.8-27b}}")
    private String groqModel;

    @Value("${groq.api.temperature:${spring.ai.openai.chat.options.temperature:0.2}}")
    private double temperature = 0.2;

    @Value("${groq.api.max-tokens:250}")
    private int maxTokens = 250;

    @Value("${groq.api.rpm-limit:25}")
    private int rpmLimit = 25;

    private final Deque<Long> peticionesUltimoMinuto = new ArrayDeque<>();

    private static final Set<String> TEMAS_PROHIBIDOS = Set.of(
            "cafe", "café", "receta", "cocina", "chiste", "tarea", "matematica", "matemática",
            "politica", "política", "futbol", "fútbol", "clima", "tiempo", "programacion",
            "programación", "codigo", "código", "musica externa", "pelicula", "película"
    );

    public ServiceChatbot(
            EventoRepository eventoRepository,
            @Value("${groq.api.base-url:${spring.ai.openai.base-url:https://api.groq.com/openai/v1}}") String baseUrl) {
        this.eventoRepository = eventoRepository;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(6));

        this.restClient = RestClient.builder()
                .baseUrl(normalizarBaseUrl(baseUrl))
                .requestFactory(requestFactory)
                .build();
    }

    public ChatbotRespuestaDTO procesarMensaje(String mensajeUsuario) {
        log.info("[CHATBOT] Recibido mensaje de usuario: '{}'", mensajeUsuario);

        if (mensajeUsuario == null || mensajeUsuario.isBlank()) {
            log.info("[CHATBOT] [NO_USA_MODELO] Mensaje vacío recibido. Respondido por regla local.");
            return ChatbotRespuestaDTO.builder()
                    .respuesta("¿En qué puedo ayudarte respecto a eventos, categorías o publicación en EventHive?")
                    .fuente("LOCAL_RULE")
                    .modoContingencia(false)
                    .build();
        }

        String msgMin = mensajeUsuario.toLowerCase().trim();

        for (String tema : TEMAS_PROHIBIDOS) {
            if (msgMin.contains(tema)) {
                log.info("[CHATBOT] [NO_USA_MODELO] [LOCAL_GUARDRAIL] Mensaje bloqueado por coincidencia con tema prohibido ('{}'). No se invoca modelo IA.", tema);
                return ChatbotRespuestaDTO.builder()
                        .respuesta("Lo siento, soy el asistente virtual exclusivo de EventHive. Solo puedo ayudarte con cartelera de eventos, compra de entradas y cómo publicar eventos.")
                        .fuente("LOCAL_GUARDRAIL")
                        .modoContingencia(false)
                        .build();
            }
        }

        if (esSaludo(msgMin)) {
            log.info("[CHATBOT] [NO_USA_MODELO] [LOCAL_RULE] Saludo detectado. Se responde con bienvenida estándar sin invocar modelo IA.");
            return ChatbotRespuestaDTO.builder()
                    .respuesta("¡Hola! Bienvenido a EventHive. ¿Tienes dudas sobre nuestra cartelera de eventos, compra de boletas o cómo registrarte como organizador?")
                    .fuente("LOCAL_RULE")
                    .modoContingencia(false)
                    .build();
        }

        if (esPreguntaOrganizador(msgMin)) {
            log.info("[CHATBOT] [NO_USA_MODELO] [LOCAL_RULE] Pregunta sobre organizador detectada. Se responde con guía local sin invocar modelo IA.");
            return ChatbotRespuestaDTO.builder()
                    .respuesta("Para publicar tus eventos en EventHive: inicia sesión, ve a tu Perfil y selecciona 'Convertirse en Organizador'. Una vez validada tu solicitud, podrás crear y gestionar tus propios eventos y localidades.")
                    .fuente("LOCAL_RULE")
                    .modoContingencia(false)
                    .build();
        }

        if (esConsultaCartelera(msgMin)) {
            log.info("[CHATBOT] [NO_USA_MODELO] [LOCAL_EVENTOS] Consulta sobre cartelera detectada. Se obtienen eventos reales de la base de datos sin invocar modelo IA.");
            return responderConEventosReales();
        }

        if (!permitirPeticionGroq()) {
            log.warn("[CHATBOT] [NO_USA_MODELO] [LIMITE_RPM] Límite preventivo de tasa alcanzado ({} RPM). Activando contingencia de emergencia.", rpmLimit);
            return ChatbotRespuestaDTO.builder()
                    .respuesta(responderConPlanDeEmergencia(msgMin, "ALTA_DEMANDA"))
                    .fuente("CONTINGENCIA")
                    .modoContingencia(true)
                    .build();
        }

        if (groqApiKey == null || groqApiKey.isBlank() || groqApiKey.contains("TuApiKey")) {
            log.warn("[CHATBOT] [NO_USA_MODELO] [SIN_API_KEY] La API Key de Groq no está configurada o es inválida. El modelo IA NO se está usando. Activando contingencia.");
            return ChatbotRespuestaDTO.builder()
                    .respuesta(responderConPlanDeEmergencia(msgMin, "SIN_LLAVE"))
                    .fuente("CONTINGENCIA")
                    .modoContingencia(true)
                    .build();
        }

        try {
            log.info("[CHATBOT] [INVOCANDO_MODELO] >>> Solicitando respuesta al modelo IA '{}' para el mensaje: '{}'", groqModel, mensajeUsuario);
            String respuestaIA = llamarGroq(mensajeUsuario);
            log.info("[CHATBOT] [IA_RESPONDE_EXITOSAMENTE] <<< El modelo IA '{}' generó respuesta con éxito. Fuente: IA_GROQ. Respuesta: '{}'", groqModel, respuestaIA);
            return ChatbotRespuestaDTO.builder()
                    .respuesta(respuestaIA)
                    .fuente("IA_GROQ")
                    .modoContingencia(false)
                    .build();
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("[CHATBOT] [CATCH_ACTIVADO] [RATE_LIMIT_429] Groq respondió 429 Too Many Requests con el modelo '{}'. Respondiendo por CATCH de contingencia. Detalle: {}", groqModel, e.getMessage());
            return ChatbotRespuestaDTO.builder()
                    .respuesta(responderConPlanDeEmergencia(msgMin, "RATE_LIMIT"))
                    .fuente("CONTINGENCIA")
                    .modoContingencia(true)
                    .build();
        } catch (ResourceAccessException e) {
            log.error("[CHATBOT] [CATCH_ACTIVADO] [TIMEOUT_RED] Timeout o problema de red al conectar con modelo IA '{}'. Respondiendo por CATCH de contingencia. Detalle: {}", groqModel, e.getMessage());
            return ChatbotRespuestaDTO.builder()
                    .respuesta(responderConPlanDeEmergencia(msgMin, "TIMEOUT"))
                    .fuente("CONTINGENCIA")
                    .modoContingencia(true)
                    .build();
        } catch (RestClientResponseException e) {
            log.error("[CHATBOT] [CATCH_ACTIVADO] [API_ERROR] Error de respuesta de Groq API ({}) para el modelo '{}'. Respondiendo por CATCH de contingencia. Cuerpo de error: {}", e.getStatusCode(), groqModel, e.getResponseBodyAsString());
            return ChatbotRespuestaDTO.builder()
                    .respuesta(responderConPlanDeEmergencia(msgMin, "API_ERROR"))
                    .fuente("CONTINGENCIA")
                    .modoContingencia(true)
                    .build();
        } catch (Exception e) {
            log.error("[CHATBOT] [CATCH_ACTIVADO] [ERROR_GENERAL] Fallo inesperado al invocar modelo IA '{}'. Respondiendo por CATCH de contingencia. Detalle: {}", groqModel, e.getMessage(), e);
            return ChatbotRespuestaDTO.builder()
                    .respuesta(responderConPlanDeEmergencia(msgMin, "ERROR_GENERAL"))
                    .fuente("CONTINGENCIA")
                    .modoContingencia(true)
                    .build();
        }
    }

    private synchronized boolean permitirPeticionGroq() {
        long ahora = System.currentTimeMillis();
        long limiteVentana = ahora - 60_000;

        while (!peticionesUltimoMinuto.isEmpty() && peticionesUltimoMinuto.peekFirst() < limiteVentana) {
            peticionesUltimoMinuto.pollFirst();
        }

        if (peticionesUltimoMinuto.size() >= rpmLimit) {
            return false;
        }

        peticionesUltimoMinuto.addLast(ahora);
        return true;
    }

    private String llamarGroq(String mensajeUsuario) {
        log.info("[CHATBOT] [LLAMANDO_GROQ] Realizando POST a /chat/completions con modelo '{}', temperature={}, max_tokens={}", groqModel, temperature, maxTokens);
        String systemPrompt = "Eres el asistente virtual oficial de EventHive, la plataforma de venta de boletería y gestión de eventos. "
                + "Tu función se limita estrictamente a: responder dudas sobre la cartelera de eventos, categorías, compra de boletas y cómo ser organizador. "
                + "REGLAS: "
                + "1. Responde de forma cordial, precisa y directa (máximo 2 a 3 oraciones). "
                + "2. Si la pregunta no se relaciona con EventHive o eventos, responde textualmente: 'Lo siento, solo puedo asistirte con información de eventos y funciones de EventHive.' "
                + "3. Nunca inventes información.";

        Map<String, Object> payload = Map.of(
                "model", groqModel,
                "temperature", temperature,
                "max_tokens", maxTokens,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", mensajeUsuario)
                )
        );

        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + groqApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        if (response != null && response.has("choices") && !response.get("choices").isEmpty()) {
            JsonNode choice = response.get("choices").get(0);
            if (choice.has("message") && choice.get("message").has("content")) {
                return choice.get("message").get("content").asText().trim();
            }
        }
        throw new IllegalStateException("Respuesta de Groq sin contenido válido");
    }

    private ChatbotRespuestaDTO responderConEventosReales() {
        try {
            List<Evento> eventos = eventoRepository.findPublicadosVisibles(PageRequest.of(0, 3)).getContent();
            if (eventos.isEmpty()) {
                return ChatbotRespuestaDTO.builder()
                        .respuesta("Actualmente estamos renovando nuestra cartelera de eventos. ¡Vuelve pronto a la pantalla principal para descubrir las novedades!")
                        .fuente("LOCAL_EVENTOS")
                        .modoContingencia(false)
                        .build();
            }

            String resumen = eventos.stream()
                    .map(e -> String.format("• %s (%s en %s)", e.getTitulo(), e.getFecha(), e.getLugar() != null ? e.getLugar() : "Lugar por confirmar"))
                    .collect(Collectors.joining("\n"));

            return ChatbotRespuestaDTO.builder()
                    .respuesta("Aquí tienes algunos de los próximos eventos destacados en EventHive:\n" + resumen + "\n\nPuedes ver más detalles y comprar tus entradas en la sección principal.")
                    .fuente("LOCAL_EVENTOS")
                    .modoContingencia(false)
                    .build();
        } catch (Exception e) {
            log.warn("No fue posible cargar eventos en vivo para el chatbot: {}", e.getMessage());
            return ChatbotRespuestaDTO.builder()
                    .respuesta("Puedes consultar toda nuestra cartelera de conciertos y eventos actualizada directamente en la pantalla de inicio de EventHive.")
                    .fuente("LOCAL_EVENTOS")
                    .modoContingencia(false)
                    .build();
        }
    }

    private static final java.util.regex.Pattern PATRON_SALUDO =
            java.util.regex.Pattern.compile("^(hola|buenas|buen dia|buenos dias|buenas tardes|buenas noches|hey|que tal|saludos).*", java.util.regex.Pattern.CASE_INSENSITIVE);

    private boolean esSaludo(String msg) {
        return PATRON_SALUDO.matcher(msg).matches();
    }

    private boolean esPreguntaOrganizador(String msg) {
        return msg.contains("organizador") || msg.contains("publicar") || msg.contains("crear evento") || msg.contains("vender boleto");
    }

    private boolean esConsultaCartelera(String msg) {
        return msg.contains("cartelera") || msg.contains("que eventos") || msg.contains("qué eventos")
                || msg.contains("proximos eventos") || msg.contains("próximos eventos") || msg.contains("eventos disponibles")
                || msg.contains("conciertos") || msg.contains("cercanos");
    }

    private String responderConPlanDeEmergencia(String mensaje, String motivo) {
        if (mensaje.contains("organizador") || mensaje.contains("publicar") || mensaje.contains("crear") || mensaje.contains("vender")) {
            return "Para publicar eventos o registrarte como organizador, ve a tu Perfil de EventHive y selecciona 'Convertirse en Organizador'.";
        }
        if (mensaje.contains("evento") || mensaje.contains("proximo") || mensaje.contains("cercano") || mensaje.contains("donde") || mensaje.contains("cartelera")) {
            return "Te invitamos a explorar la cartelera completa de eventos y conciertos disponibles directamente desde la pantalla principal de EventHive.";
        }
        if (mensaje.contains("boleto") || mensaje.contains("entrada") || mensaje.contains("comprar") || mensaje.contains("pago")) {
            return "Puedes adquirir tus boletas seleccionando el evento deseado, eligiendo tu localidad y completando el pago seguro en línea. Tus entradas contarán con código QR verificable.";
        }
        if (mensaje.contains("hola") || mensaje.contains("buenas") || mensaje.contains("saludos")) {
            return "¡Hola! Bienvenido a EventHive. ¿Tienes dudas sobre nuestra cartelera de eventos o sobre cómo registrarte como organizador?";
        }
        return "Gracias por consultar a EventHive. En este momento puedes navegar la cartelera completa y comprar tus boletas desde el menú principal de la plataforma.";
    }

    // METODOS AUXILIARES Y MAPEO
    private static String normalizarBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "https://api.groq.com/openai/v1";
        }
        String clean = url.trim();
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://" + clean;
        }
        if (clean.endsWith("/chat/completions")) {
            clean = clean.substring(0, clean.length() - "/chat/completions".length());
        }
        if (!clean.contains("/openai")) {
            clean = clean.replaceAll("/+$", "") + "/openai/v1";
        }
        return clean;
    }
}
