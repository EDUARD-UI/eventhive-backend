package com.eventhive.app.security.auth;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.eventhive.app.dto.response.LoginResponseDTO;
import com.eventhive.app.exception.GlobalExceptionHandler;
import com.eventhive.app.service.ServiceAutenticacion;
import com.eventhive.app.service.ServiceRecuperacionClave;
import com.eventhive.app.service.ServiceSolicitudVerificacion;
import com.eventhive.app.service.ServiceUsuario;

@ExtendWith(MockitoExtension.class)
class AuthApiControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ServiceAutenticacion serviceAutenticacion;

    @Mock
    private ServiceRecuperacionClave serviceRecuperacionClave;

    @Mock
    private ServiceSolicitudVerificacion serviceSolicitud;

    @Mock
    private ServiceUsuario serviceUsuario;

    @InjectMocks
    private AuthApiController authApiController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authApiController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/refresh exitoso renueva token y retorna HTTP 200")
    void refreshExitoso() throws Exception {
        LoginResponseDTO dto = LoginResponseDTO.builder()
                .id(1L)
                .accessToken("new.access.token")
                .refreshToken("valid.refresh.token")
                .tipo("Bearer")
                .correo("user@eventhive.com")
                .rol("CLIENTE")
                .build();

        when(serviceAutenticacion.refrescarToken(eq("valid.refresh.token"))).thenReturn(dto);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"valid.refresh.token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new.access.token"))
                .andExpect(jsonPath("$.data.refreshToken").value("valid.refresh.token"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh acepta snake_case refresh_token y retorna HTTP 200")
    void refreshAceptaSnakeCase() throws Exception {
        LoginResponseDTO dto = LoginResponseDTO.builder()
                .id(1L)
                .accessToken("new.access.token")
                .refreshToken("valid.refresh.token")
                .tipo("Bearer")
                .correo("user@eventhive.com")
                .rol("CLIENTE")
                .build();

        when(serviceAutenticacion.refrescarToken(eq("valid.refresh.token"))).thenReturn(dto);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"valid.refresh.token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new.access.token"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh con token expirado retorna HTTP 401 Unauthorized (y no 400)")
    void refreshConTokenExpiradoRetorna401() throws Exception {
        when(serviceAutenticacion.refrescarToken(eq("expired.refresh.token")))
                .thenThrow(new BadCredentialsException("Refresh token expirado. Por favor, inicia sesión nuevamente."));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"expired.refresh.token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.mensaje").value("Refresh token expirado. Por favor, inicia sesión nuevamente."));
    }

    @Test
    @DisplayName("POST /api/auth/refresh acepta token en header Authorization Bearer")
    void refreshConBearerHeader() throws Exception {
        LoginResponseDTO dto = LoginResponseDTO.builder()
                .id(1L)
                .accessToken("new.access.token")
                .refreshToken("valid.bearer.refresh")
                .tipo("Bearer")
                .correo("user@eventhive.com")
                .rol("CLIENTE")
                .build();

        when(serviceAutenticacion.refrescarToken(eq("valid.bearer.refresh"))).thenReturn(dto);

        mockMvc.perform(post("/api/auth/refresh")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid.bearer.refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new.access.token"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh sin token retorna HTTP 401 Unauthorized")
    void refreshSinTokenRetorna401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
