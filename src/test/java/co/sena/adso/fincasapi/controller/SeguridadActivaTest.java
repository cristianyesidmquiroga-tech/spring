package co.sena.adso.fincasapi.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.fincasapi.config.SecurityConfig;
import co.sena.adso.fincasapi.repository.UsuarioRepository;
import co.sena.adso.fincasapi.service.FincaService;
import co.sena.adso.fincasapi.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Con la seguridad encendida (como en producción) las rutas de datos piden token. */
@WebMvcTest({FincaController.class, HelloController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.security.enabled=true")
class SeguridadActivaTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FincaService fincaService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Test
    void sinTokenLaRutaDeFincasDevuelve401() throws Exception {
        mockMvc.perform(get("/api/fincas")).andExpect(status().isUnauthorized());
    }

    @Test
    void helloSigueSiendoPublico() throws Exception {
        mockMvc.perform(get("/api/hello")).andExpect(status().isOk());
    }
}
