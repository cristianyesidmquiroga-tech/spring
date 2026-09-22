package co.sena.adso.fincasapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.fincasapi.config.SecurityConfig;
import co.sena.adso.fincasapi.dto.FincaCultivoRequestDTO;
import co.sena.adso.fincasapi.exception.BusinessException;
import co.sena.adso.fincasapi.repository.UsuarioRepository;
import co.sena.adso.fincasapi.service.FincaCultivoService;
import co.sena.adso.fincasapi.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FincaCultivoController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.security.enabled=false")
class FincaCultivoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FincaCultivoService siembraService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Test
    void siembraQueNoCabeEnLaFincaDevuelve422() throws Exception {
        when(siembraService.crear(any(FincaCultivoRequestDTO.class)))
                .thenThrow(new BusinessException("El área sembrada (999.0 ha) es mayor que la finca El Recreo (12.5 ha)"));

        String cuerpo = """
                {"fincaId":1,"cultivoId":1,"areaSembradaHa":999.0,"fechaSiembra":"2026-03-15",
                 "temporada":"PRIMAVERA","estado":"ACTIVO"}
                """;

        mockMvc.perform(post("/api/finca-cultivos").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void temporadaQueNoExisteDevuelve400() throws Exception {
        String cuerpo = """
                {"fincaId":1,"cultivoId":1,"areaSembradaHa":2.0,"fechaSiembra":"2026-03-15",
                 "temporada":"MONZON","estado":"ACTIVO"}
                """;

        mockMvc.perform(post("/api/finca-cultivos").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
    }
}
