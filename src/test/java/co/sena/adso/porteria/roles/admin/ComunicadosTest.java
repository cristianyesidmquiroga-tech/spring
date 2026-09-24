package co.sena.adso.porteria.roles.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/admin/test_comunicados.py
class ComunicadosTest extends PruebaRol {

    @Test
    void entraAComunicados() throws Exception {
        mvc.perform(con(sesion(), get("/api/comunicados"))).andExpect(status().isOk());
    }

    @Test
    void enviarSinDestinatariosSeRechaza() throws Exception {
        mvc.perform(conJson(sesion(), post("/api/comunicados"),
                Map.of("tipo", "Comunicado General", "destinatarios", List.of()))).andExpect(status().isBadRequest());
    }
}
