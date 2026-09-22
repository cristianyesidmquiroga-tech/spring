package co.sena.adso.fincasapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.fincasapi.config.SecurityConfig;
import co.sena.adso.fincasapi.dto.FincaRequestDTO;
import co.sena.adso.fincasapi.dto.FincaResponseDTO;
import co.sena.adso.fincasapi.exception.ResourceNotFoundException;
import co.sena.adso.fincasapi.repository.UsuarioRepository;
import co.sena.adso.fincasapi.service.FincaService;
import co.sena.adso.fincasapi.service.JwtService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FincaController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.security.enabled=false")
class FincaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FincaService fincaService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    private static final String FINCA_VALIDA = """
            {"nombre":"La Aurora","propietario":"Ana","vereda":"El Centro","municipio":"Vélez","hectareas":6.5}
            """;

    @Test
    void listarDevuelve200ConLasFincas() throws Exception {
        when(fincaService.listar()).thenReturn(List.of(
                new FincaResponseDTO(1L, "El Recreo", "Luis", "Alto Jordán", "Vélez", 12.5)));

        mockMvc.perform(get("/api/fincas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("El Recreo"));
    }

    @Test
    void crearDevuelve201YLaUbicacion() throws Exception {
        when(fincaService.crear(any(FincaRequestDTO.class)))
                .thenReturn(new FincaResponseDTO(5L, "La Aurora", "Ana", "El Centro", "Vélez", 6.5));

        mockMvc.perform(post("/api/fincas").contentType(MediaType.APPLICATION_JSON).content(FINCA_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/fincas/5"))
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void crearConDatosInvalidosDevuelve400ConLosCampos() throws Exception {
        String invalida = """
                {"nombre":"","propietario":"Ana","vereda":"El Centro","municipio":"Vélez","hectareas":-1}
                """;

        mockMvc.perform(post("/api/fincas").contentType(MediaType.APPLICATION_JSON).content(invalida))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.length()").value(2));
    }

    @Test
    void obtenerUnaFincaInexistenteDevuelve404() throws Exception {
        when(fincaService.obtener(9999L)).thenThrow(new ResourceNotFoundException("una finca", 9999L));

        mockMvc.perform(get("/api/fincas/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe una finca con id 9999"));
    }

    @Test
    void actualizarDevuelve200ConLosDatosNuevos() throws Exception {
        when(fincaService.actualizar(eq(1L), any(FincaRequestDTO.class)))
                .thenReturn(new FincaResponseDTO(1L, "La Aurora", "Ana", "El Centro", "Vélez", 6.5));

        mockMvc.perform(put("/api/fincas/1").contentType(MediaType.APPLICATION_JSON).content(FINCA_VALIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hectareas").value(6.5));
    }

    @Test
    void eliminarDevuelve204() throws Exception {
        mockMvc.perform(delete("/api/fincas/1")).andExpect(status().isNoContent());
    }

    @Test
    void eliminarUnaFincaInexistenteDevuelve404() throws Exception {
        doThrow(new ResourceNotFoundException("una finca", 50L)).when(fincaService).eliminar(50L);

        mockMvc.perform(delete("/api/fincas/50")).andExpect(status().isNotFound());
    }
}
