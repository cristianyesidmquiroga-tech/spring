package co.sena.adso.porteria.roles.administrador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import java.util.Map;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/administrador/test_ayuda.py
class AyudaTest extends PruebaRol {

    @Test
    void entraAlCentroDeAyuda() throws Exception {
        mvc.perform(con(sesion(), get("/api/ayuda"))).andExpect(status().isOk());
    }

    @Test
    void contactaAUnAsesor() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(conJson(sesion, post("/api/ayuda/contacto"), Map.of("asunto", "Otro", "detalle", "Necesito ayuda")))
                .andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM mensajes WHERE usuario_id = ?", Long.class,
                sesion.usuario().getId())).isEqualTo(1);
    }
}
