package co.sena.adso.porteria.roles.celador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import co.sena.adso.porteria.soporte.Perfil;
import java.util.Map;

// Portería 2: tests/roles/celador/test_ayuda.py
class AyudaTest extends PruebaRol {

    @CeladorYPorteria
    void entraAlCentroDeAyuda(Perfil perfil) throws Exception {
        mvc.perform(con(entrarComo(perfil), get("/api/ayuda"))).andExpect(status().isOk());
    }

    @CeladorYPorteria
    void contactaAUnAsesor(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(conJson(sesion, post("/api/ayuda/contacto"), Map.of("asunto", "Otro", "detalle", "Necesito ayuda")))
                .andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM mensajes WHERE usuario_id = ?", Long.class,
                sesion.usuario().getId())).isEqualTo(1);
    }
}
