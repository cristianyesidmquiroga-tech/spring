package co.sena.adso.porteria.roles.trabajador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import java.util.Map;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/trabajador/test_mensajes.py
class MensajesTest extends PruebaRol {

    private long mensajesDe(Sesion sesion) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM mensajes WHERE usuario_id = ?", Long.class,
                sesion.usuario().getId());
    }

    @Test
    void entraASusMensajes() throws Exception {
        mvc.perform(con(sesion(), get("/api/mensajes"))).andExpect(status().isOk());
    }

    @Test
    void enviaUnMensaje() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(conJson(sesion, post("/api/mensajes"), Map.of("texto", "Hola"))).andExpect(status().isCreated());
        assertThat(mensajesDe(sesion)).isEqualTo(1);
    }

    @Test
    void mensajeVacioNoSeEnvia() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(conJson(sesion, post("/api/mensajes"), Map.of("texto", "   ")));
        assertThat(mensajesDe(sesion)).isZero();
    }
}
