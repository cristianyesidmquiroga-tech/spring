package co.sena.adso.porteria.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.config.SecurityConfig;
import co.sena.adso.porteria.dto.AutorizacionRequestDTO;
import co.sena.adso.porteria.exception.BusinessException;
import co.sena.adso.porteria.repository.UsuarioRepository;
import co.sena.adso.porteria.service.JwtService;
import co.sena.adso.porteria.service.UsuarioAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminUsuarioController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.security.enabled=true")
class AdminUsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioAdminService usuarioAdminService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Test
    void sinSesionResponde401() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"USUARIO", "OPERAR_PORTERIA"})
    void unCeladorNoEntraALaGestionDeUsuarios() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"USUARIO", "ADMIN"})
    void elAdminListaUsuarios() throws Exception {
        when(usuarioAdminService.listar(any(), any(), any(), any())).thenReturn(Page.empty());
        mockMvc.perform(get("/api/admin/usuarios")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"USUARIO", "ADMIN"})
    void crearSinNombreNiCorreoResponde400ConLosCampos() throws Exception {
        mockMvc.perform(post("/api/admin/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"correo\":\"no-es-correo\",\"rolId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.length()").value(2));
    }

    @Test
    @WithMockUser(authorities = {"USUARIO", "ADMIN"})
    void campoQueNoEstaEnElContratoResponde400() throws Exception {
        mockMvc.perform(post("/api/admin/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"correo\":\"ana@x.co\",\"rolId\":2,\"esAdmin\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"USUARIO", "ADMIN"})
    void eliminarUnAdminResponde422() throws Exception {
        doThrow(new BusinessException("Las cuentas con rol Admin no se eliminan, solo se editan"))
                .when(usuarioAdminService).eliminar(eq(1L), any(AutorizacionRequestDTO.class));
        mockMvc.perform(delete("/api/admin/usuarios/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorizadoPor\":\"x\",\"motivo\":\"x\"}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
