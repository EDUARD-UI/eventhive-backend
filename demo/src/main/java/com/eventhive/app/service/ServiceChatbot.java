package com.eventhive.app.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

import com.eventhive.app.dto.response.ChatbotEventoDTO;
import com.eventhive.app.dto.response.ChatbotRespuestaDTO;
import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.model.Categoria;
import com.eventhive.app.model.Evento;
import com.eventhive.app.repository.CategoriaRepository;
import com.eventhive.app.repository.EventoRepository;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class ServiceChatbot {

    private static final Logger log = LoggerFactory.getLogger(ServiceChatbot.class);
    private static final Locale LOCALE_ES = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter FORMATO_FECHA_ES = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", LOCALE_ES);

    private final EventoRepository eventoRepository;
    private final CategoriaRepository categoriaRepository;
    private final RestClient restClient;

    @Value("${groq.api.key:${spring.ai.openai.api-key:}}")
    private String groqApiKey;

    @Value("${groq.api.model:${spring.ai.openai.chat.options.model:qwen/qwen3.8-27b}}")
    private String groqModel;

    @Value("${groq.api.temperature:${spring.ai.openai.chat.options.temperature:0.2}}")
    private double temperature = 0.2;

    @Value("${groq.api.max-tokens:350}")
    private int maxTokens = 350;

    @Value("${groq.api.rpm-limit:25}")
    private int rpmLimit = 25;

    private final Deque<Long> peticionesUltimoMinuto = new ArrayDeque<>();

    private static final Set<String> TEMAS_PROHIBIDOS = Set.of(
            "cafe", "café", "receta", "cocina", "chiste", "tarea", "matematica", "matemática",
            "politica", "política", "futbol", "fútbol", "clima", "tiempo", "programacion",
            "programación", "codigo", "código", "pelicula", "película"
    );

    private static final Map<String, Integer> MESES_MAP = Map.ofEntries(
            Map.entry("enero", 1), Map.entry("febrero", 2), Map.entry("marzo", 3),
            Map.entry("abril", 4), Map.entry("mayo", 5), Map.entry("junio", 6),
            Map.entry("julio", 7), Map.entry("agosto", 8), Map.entry("septiembre", 9),
            Map.entry("setiembre", 9), Map.entry("octubre", 10), Map.entry("noviembre", 11),
            Map.entry("diciembre", 12)
    );

    public ServiceChatbot(
            EventoRepository eventoRepository,
            CategoriaRepository categoriaRepository,
            @Value("${groq.api.base-url:${spring.ai.openai.base-url:https://api.groq.com/openai/v1}}") String baseUrl) {
        this.eventoRepository = eventoRepository;
        this.categoriaRepository = categoriaRepository;

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
                    .respuesta("¿En qué puedo ayudarte respecto a eventos, fechas o categorías en EventHive?")
                    .fuente("LOCAL_RULE")
                    .modoContingencia(false)
                    .build();
        }

        String msgMin = mensajeUsuario.toLowerCase(LOCALE_ES).trim();

        // 1. Guardrail de temas prohibidos
        for (String tema : TEMAS_PROHIBIDOS) {
            if (msgMin.contains(tema)) {
                log.info("[CHATBOT] [NO_USA_MODELO] [LOCAL_GUARDRAIL] Mensaje bloqueado por coincidencia con tema prohibido ('{}').", tema);
                return ChatbotRespuestaDTO.builder()
                        .respuesta("Lo siento, soy el asistente virtual exclusivo de EventHive. Solo puedo ayudarte con información de eventos, fechas, categorías y compra de entradas.")
                        .fuente("LOCAL_GUARDRAIL")
                        .modoContingencia(false)
                        .build();
            }
        }

        // 2. Saludos
        if (esSaludo(msgMin)) {
            log.info("[CHATBOT] [NO_USA_MODELO] [LOCAL_RULE] Saludo detectado.");
            return ChatbotRespuestaDTO.builder()
                    .respuesta("¡Hola! Bienvenido a EventHive. ¿Te gustaría consultar conciertos, eventos por fecha, categorías disponibles o cómo comprar tus boletas?")
                    .fuente("LOCAL_RULE")
                    .modoContingencia(false)
                    .build();
        }

        // 3. Pregunta organizador
        if (esPreguntaOrganizador(msgMin)) {
            log.info("[CHATBOT] [NO_USA_MODELO] [LOCAL_RULE] Pregunta sobre organizador detectada.");
            return ChatbotRespuestaDTO.builder()
                    .respuesta("Para publicar tus eventos en EventHive: inicia sesión, ve a tu Perfil y selecciona 'Convertirse en Organizador'. Una vez validada tu solicitud, podrás crear y gestionar tus propios eventos y localidades.")
                    .fuente("LOCAL_RULE")
                    .modoContingencia(false)
                    .build();
        }

        // 4. Preguntas específicas sobre cartelera, categorías, fechas o conciertos
        boolean esConsultaCarteleraOFechas = esConsultaCartelera(msgMin);

        // Si tenemos IA Groq activa y con cuota disponible, aprovechamos el modelo con contexto enriquecido
        if (permitirPeticionGroq() && groqApiKey != null && !groqApiKey.isBlank() && !groqApiKey.contains("TuApiKey")) {
            try {
                log.info("[CHATBOT] [INVOCANDO_MODELO] Solicitando respuesta al modelo IA '{}' para: '{}'", groqModel, mensajeUsuario);
                ChatbotRespuestaDTO respuestaIA = llamarGroqConContexto(mensajeUsuario, msgMin);
                log.info("[CHATBOT] [IA_RESPONDE_EXITOSAMENTE] Modelo respondió con éxito.");
                return respuestaIA;
            } catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("[CHATBOT] [RATE_LIMIT_429] Groq respondió 429 Too Many Requests.");
            } catch (ResourceAccessException e) {
                log.error("[CHATBOT] [TIMEOUT_RED] Timeout al conectar con Groq: {}", e.getMessage());
            } catch (RestClientResponseException e) {
                log.error("[CHATBOT] [API_ERROR] Error de respuesta de Groq API: {}", e.getResponseBodyAsString());
            } catch (Exception e) {
                log.error("[CHATBOT] [ERROR_GENERAL] Fallo inesperado en Groq: {}", e.getMessage(), e);
            }
        }

        // 5. Respuesta local inteligente (si IA no disponible o falló)
        if (esConsultaCarteleraOFechas) {
            log.info("[CHATBOT] Resolviendo consulta de eventos/categorías localmente con base de datos.");
            return resolverConsultaEventosLocal(msgMin);
        }

        // Plan de contingencia general
        return ChatbotRespuestaDTO.builder()
                .respuesta(responderConPlanDeEmergencia(msgMin))
                .fuente("CONTINGENCIA")
                .modoContingencia(true)
                .build();
    }

    private ChatbotRespuestaDTO llamarGroqConContexto(String mensajeUsuario, String msgMin) {
        LocalDate hoy = LocalDate.now();
        String fechaHoyFormateada = hoy.format(FORMATO_FECHA_ES);
        String diaSemana = hoy.getDayOfWeek().getDisplayName(TextStyle.FULL, LOCALE_ES);

        // Obtener categorías registradas
        List<Categoria> categoriasList = categoriaRepository.findAll();
        String categoriasStr = categoriasList.stream()
                .map(Categoria::getNombre)
                .collect(Collectors.joining(", "));

        // Obtener eventos publicados
        List<Evento> eventosPublicados = eventoRepository.findAllPublicadosParaChatbot(PageRequest.of(0, 35));

        StringBuilder catalogoEventos = new StringBuilder();
        for (Evento e : eventosPublicados) {
            String catNom = e.getCategoria() != null ? e.getCategoria().getNombre() : "General";
            String fechaEv = e.getFecha() != null ? e.getFecha().format(FORMATO_FECHA_ES) : "Fecha por confirmar";
            catalogoEventos.append(String.format("- ID: %d | Título: %s | Categoría: %s | Fecha: %s (%s) | Hora: %s | Lugar: %s\n",
                    e.getId(),
                    e.getTitulo(),
                    catNom,
                    e.getFecha(),
                    fechaEv,
                    e.getHora() != null ? e.getHora().toString() : "N/A",
                    e.getLugar() != null ? e.getLugar() : "Lugar por confirmar"
            ));
        }

        String systemPrompt = String.format("""
                Eres HiveBot, el asistente virtual oficial de EventHive, la plataforma de venta de boletería y gestión de eventos.
                FECHA ACTUAL DEL SISTEMA: Hoy es %s, %s (Año: %d).
                CATEGORÍAS DE EVENTOS DISPONIBLES EN EVENTHIVE: %s.

                CATÁLOGO DE EVENTOS REALES PUBLICADOS EN EVENTHIVE (SOLO USA ESTOS):
                %s

                REGLAS OBLIGATORIAS:
                1. SOLO responde sobre eventos que estén en la lista anterior (todos ellos están PUBLICADOS). NUNCA inventes eventos, fechas ni lugares.
                2. Si el usuario pregunta por conciertos más cercanos, o eventos de una categoría específica (ej. conciertos, festivales, etc.) o en un mes específico (ej. mes X, noviembre, octubre), responde de acuerdo a la consulta filtrando únicamente los eventos correspondientes.
                3. Al mencionar cualquier evento, formatea su nombre OBLIGATORIAMENTE como un enlace Markdown hacia su página de detalle: `[Nombre del Evento](/eventos/ID)`, seguido de la fecha exacta y lugar.
                   Ejemplo: "Los eventos de la categoría Conciertos para noviembre son:\\n• [Festival de Salsa](/eventos/4) - 15 de noviembre de 2026 en Plaza Central"
                4. Si el usuario pregunta por la fecha de hoy o fechas de eventos, responde con exactitud considerando que hoy es %s.
                5. Si no hay eventos publicados para la categoría o mes solicitado, indícalo amablemente invitándolo a explorar otras fechas o categorías.
                6. Mantén las respuestas cordiales, breves y directas (máximo 3 a 4 oraciones).
                7. Si la pregunta no se relaciona con EventHive ni eventos, responde: 'Lo siento, solo puedo asistirte con información de eventos, fechas, categorías y compra de entradas en EventHive.'
                """,
                diaSemana, fechaHoyFormateada, hoy.getYear(),
                categoriasStr.isEmpty() ? "Conciertos, Festivales, Teatro, Conferencias, Deportes" : categoriasStr,
                catalogoEventos.length() > 0 ? catalogoEventos.toString() : "Actualmente no hay eventos publicados en el catálogo.",
                fechaHoyFormateada
        );

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

        String textoRespuesta = "";
        if (response != null && response.has("choices") && !response.get("choices").isEmpty()) {
            JsonNode choice = response.get("choices").get(0);
            if (choice.has("message") && choice.get("message").has("content")) {
                textoRespuesta = choice.get("message").get("content").asText().trim();
            }
        }

        if (textoRespuesta.isBlank()) {
            throw new IllegalStateException("Respuesta de Groq sin contenido válido");
        }

        // Extraer eventos que coincidan con los IDs mencionados en el markdown `[...](/eventos/ID)` o con la consulta
        List<ChatbotEventoDTO> eventosEncontrados = extraerEventosMencionados(textoRespuesta, eventosPublicados);
        if (eventosEncontrados.isEmpty()) {
            // Intentar enlazar eventos por palabras clave de la consulta
            eventosEncontrados = buscarEventosPorConsulta(msgMin, eventosPublicados);
        }

        return ChatbotRespuestaDTO.builder()
                .respuesta(textoRespuesta)
                .fuente("IA_GROQ")
                .modoContingencia(false)
                .eventos(eventosEncontrados)
                .build();
    }

    public ChatbotRespuestaDTO resolverConsultaEventosLocal(String msgMin) {
        LocalDate hoy = LocalDate.now();
        List<Categoria> categorias = categoriaRepository.findAll();

        // 1. Detectar si pregunta por la fecha actual
        if (msgMin.contains("que fecha es") || msgMin.contains("qué fecha es") || msgMin.contains("fecha de hoy") || msgMin.contains("que dia es") || msgMin.contains("qué día es")) {
            String diaSemana = hoy.getDayOfWeek().getDisplayName(TextStyle.FULL, LOCALE_ES);
            String fechaStr = hoy.format(FORMATO_FECHA_ES);
            return ChatbotRespuestaDTO.builder()
                    .respuesta(String.format("Hoy es %s, %s. ¿Deseas consultar los próximos eventos o conciertos a partir de hoy?", diaSemana, fechaStr))
                    .fuente("LOCAL_RULE")
                    .modoContingencia(false)
                    .build();
        }

        // 2. Detectar si pregunta por las categorías disponibles
        if (msgMin.contains("categorias") || msgMin.contains("categorías") || msgMin.contains("que tipo de eventos") || msgMin.contains("qué tipo de eventos")) {
            String nombresCategorias = categorias.stream().map(Categoria::getNombre).collect(Collectors.joining(", "));
            return ChatbotRespuestaDTO.builder()
                    .respuesta("En EventHive contamos con eventos en las siguientes categorías: " + nombresCategorias + ". Puedes preguntarme por eventos específicos de cualquiera de ellas.")
                    .fuente("LOCAL_RULE")
                    .modoContingencia(false)
                    .build();
        }

        // 3. Detectar categoría mencionada
        String categoriaDetectada = null;
        for (Categoria cat : categorias) {
            String nombreCatMin = cat.getNombre().toLowerCase(LOCALE_ES);
            if (msgMin.contains(nombreCatMin)) {
                categoriaDetectada = cat.getNombre();
                break;
            }
        }
        if (categoriaDetectada == null) {
            if (msgMin.contains("concierto") || msgMin.contains("conciertos") || msgMin.contains("musica") || msgMin.contains("música")) {
                categoriaDetectada = "Concierto";
            } else if (msgMin.contains("teatro") || msgMin.contains("obra")) {
                categoriaDetectada = "Teatro";
            } else if (msgMin.contains("festival") || msgMin.contains("festivales")) {
                categoriaDetectada = "Festival";
            } else if (msgMin.contains("deporte") || msgMin.contains("deportes")) {
                categoriaDetectada = "Deporte";
            } else if (msgMin.contains("conferencia") || msgMin.contains("conferencias") || msgMin.contains("seminario")) {
                categoriaDetectada = "Conferencia";
            }
        }

        // 4. Detectar mes mencionado
        Integer mesDetectado = null;
        String nombreMesDetectado = null;
        for (Map.Entry<String, Integer> entry : MESES_MAP.entrySet()) {
            if (msgMin.contains(entry.getKey())) {
                mesDetectado = entry.getValue();
                nombreMesDetectado = entry.getKey();
                break;
            }
        }
        if (mesDetectado == null && msgMin.contains("este mes")) {
            mesDetectado = hoy.getMonthValue();
            nombreMesDetectado = hoy.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES);
        }

        // 5. Detectar si pide los "más cercanos" o próximos
        boolean pideCercanos = msgMin.contains("cercan") || msgMin.contains("proxim") || msgMin.contains("próxim")
                || msgMin.contains("siguient") || msgMin.contains("primer");

        // Realizar la búsqueda en base de datos
        List<Evento> eventos;
        String mensajeEncabezado;

        if (mesDetectado != null) {
            int anio = (mesDetectado < hoy.getMonthValue()) ? hoy.getYear() + 1 : hoy.getYear();
            LocalDate inicioMes = LocalDate.of(anio, mesDetectado, 1);
            LocalDate finMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());

            eventos = eventoRepository.findPublicadosFiltroChatbot(categoriaDetectada, inicioMes, finMes, PageRequest.of(0, 5));

            String catTexto = (categoriaDetectada != null) ? "la categoría " + categoriaDetectada : "nuestra cartelera";
            mensajeEncabezado = String.format("Los eventos de %s para el mes de %s son (solo eventos publicados):",
                    catTexto, nombreMesDetectado != null ? capitalizar(nombreMesDetectado) : "solicitado");

        } else if (categoriaDetectada != null) {
            eventos = eventoRepository.findPublicadosFiltroChatbot(categoriaDetectada, hoy, null, PageRequest.of(0, 5));
            if (eventos.isEmpty()) {
                eventos = eventoRepository.findPublicadosFiltroChatbot(categoriaDetectada, null, null, PageRequest.of(0, 5));
            }
            mensajeEncabezado = String.format("Los eventos más cercanos de la categoría %s son:", categoriaDetectada);

        } else if (pideCercanos) {
            eventos = eventoRepository.findProximosPublicados(hoy, LocalTime.MIN, PageRequest.of(0, 5)).getContent();
            mensajeEncabezado = "Los eventos publicados más cercanos en EventHive son:";
        } else {
            eventos = eventoRepository.findProximosPublicados(hoy, LocalTime.MIN, PageRequest.of(0, 5)).getContent();
            if (eventos.isEmpty()) {
                eventos = eventoRepository.findAllPublicadosParaChatbot(PageRequest.of(0, 5));
            }
            mensajeEncabezado = "Aquí tienes algunos de los próximos eventos destacados en EventHive:";
        }

        if (eventos.isEmpty()) {
            String detalleSinEventos;
            if (categoriaDetectada != null && mesDetectado != null) {
                detalleSinEventos = String.format("Actualmente no hay eventos publicados en la categoría %s para el mes de %s. ¡Vuelve pronto a consultar la cartelera!",
                        categoriaDetectada, nombreMesDetectado != null ? capitalizar(nombreMesDetectado) : "");
            } else if (categoriaDetectada != null) {
                detalleSinEventos = String.format("Por el momento no hay eventos publicados en la categoría %s. Te invitamos a explorar otras categorías disponibles.", categoriaDetectada);
            } else {
                detalleSinEventos = "Actualmente estamos renovando nuestra cartelera de eventos. ¡Vuelve pronto a la pantalla principal para descubrir las novedades!";
            }

            return ChatbotRespuestaDTO.builder()
                    .respuesta(detalleSinEventos)
                    .fuente("LOCAL_EVENTOS")
                    .modoContingencia(false)
                    .eventos(List.of())
                    .build();
        }

        List<ChatbotEventoDTO> dtos = eventos.stream()
                .map(this::toChatbotEventoDTO)
                .toList();

        StringBuilder sb = new StringBuilder(mensajeEncabezado).append("\n");
        for (Evento e : eventos) {
            String fechaFormateada = e.getFecha() != null ? e.getFecha().format(FORMATO_FECHA_ES) : "Fecha por confirmar";
            String lugarStr = e.getLugar() != null ? e.getLugar() : "Lugar por confirmar";
            sb.append(String.format("• [%s](/eventos/%d) - %s en %s\n", e.getTitulo(), e.getId(), fechaFormateada, lugarStr));
        }
        sb.append("\nPuedes hacer clic en cualquier evento para ver sus detalles y localidades.");

        return ChatbotRespuestaDTO.builder()
                .respuesta(sb.toString().trim())
                .fuente("LOCAL_EVENTOS")
                .modoContingencia(false)
                .eventos(dtos)
                .build();
    }

    private List<ChatbotEventoDTO> extraerEventosMencionados(String textoRespuesta, List<Evento> catalogo) {
        Pattern pattern = Pattern.compile("/eventos/(\\d+)");
        Matcher matcher = pattern.matcher(textoRespuesta);
        Set<Long> ids = new LinkedHashSet<>();
        while (matcher.find()) {
            try {
                ids.add(Long.parseLong(matcher.group(1)));
            } catch (NumberFormatException ignored) {}
        }

        if (ids.isEmpty()) {
            return List.of();
        }

        return catalogo.stream()
                .filter(e -> ids.contains(e.getId()))
                .map(this::toChatbotEventoDTO)
                .toList();
    }

    private List<ChatbotEventoDTO> buscarEventosPorConsulta(String msgMin, List<Evento> catalogo) {
        return catalogo.stream()
                .filter(e -> {
                    String titMin = e.getTitulo().toLowerCase(LOCALE_ES);
                    String catMin = e.getCategoria() != null ? e.getCategoria().getNombre().toLowerCase(LOCALE_ES) : "";
                    return msgMin.contains(titMin) || (!catMin.isEmpty() && msgMin.contains(catMin));
                })
                .limit(5)
                .map(this::toChatbotEventoDTO)
                .toList();
    }

    private ChatbotEventoDTO toChatbotEventoDTO(Evento e) {
        String fechaFormateada = e.getFecha() != null ? e.getFecha().format(FORMATO_FECHA_ES) : "Fecha por confirmar";
        String catNombre = e.getCategoria() != null ? e.getCategoria().getNombre() : "General";
        return ChatbotEventoDTO.builder()
                .id(e.getId())
                .title(e.getTitulo())
                .nombre(e.getTitulo())
                .fecha(fechaFormateada)
                .date(fechaFormateada)
                .lugar(e.getLugar() != null ? e.getLugar() : "Lugar por confirmar")
                .categoria(catNombre)
                .url("/eventos/" + e.getId())
                .build();
    }

    private static final Pattern PATRON_SALUDO =
            Pattern.compile("^(hola|buenas|buen dia|buenos dias|buenas tardes|buenas noches|hey|que tal|saludos).*", Pattern.CASE_INSENSITIVE);

    private boolean esSaludo(String msg) {
        return PATRON_SALUDO.matcher(msg).matches();
    }

    private boolean esPreguntaOrganizador(String msg) {
        return msg.contains("organizador") || msg.contains("publicar") || msg.contains("crear evento") || msg.contains("vender boleto");
    }

    private boolean esConsultaCartelera(String msg) {
        return msg.contains("cartelera") || msg.contains("que eventos") || msg.contains("qué eventos")
                || msg.contains("proximos") || msg.contains("próximos") || msg.contains("eventos disponibles")
                || msg.contains("concierto") || msg.contains("conciertos") || msg.contains("cercano") || msg.contains("cercanos")
                || msg.contains("fecha") || msg.contains("cuando") || msg.contains("cuándo")
                || msg.contains("categoria") || msg.contains("categoría") || msg.contains("mes")
                || msg.contains("teatro") || msg.contains("festival") || msg.contains("deporte");
    }

    private String responderConPlanDeEmergencia(String mensaje) {
        if (mensaje.contains("organizador") || mensaje.contains("publicar") || mensaje.contains("crear") || mensaje.contains("vender")) {
            return "Para publicar eventos o registrarte como organizador, ve a tu Perfil de EventHive y selecciona 'Convertirse en Organizador'.";
        }
        if (mensaje.contains("evento") || mensaje.contains("proximo") || mensaje.contains("cercano") || mensaje.contains("donde") || mensaje.contains("cartelera")) {
            return "Te invitamos a explorar la cartelera completa de eventos y conciertos disponibles directamente desde la pantalla principal de EventHive.";
        }
        if (mensaje.contains("boleto") || mensaje.contains("entrada") || mensaje.contains("comprar") || mensaje.contains("pago")) {
            return "Puedes adquirir tus boletas seleccionando el evento deseado, eligiendo tu localidad y completando el pago seguro en línea. Tus entradas contarán con código QR verificable.";
        }
        return "Gracias por consultar a EventHive. En este momento puedes navegar la cartelera completa, consultar eventos por categorías y fechas, o comprar tus boletas desde la plataforma.";
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

    private static String capitalizar(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
    }

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
