package co.sena.adso.porteria.roles.celador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import co.sena.adso.porteria.soporte.Perfil;
import java.util.Map;

// Portería 2: tests/roles/celador/test_mensajes.py
class MensajesTest extends PruebaRol {

    private long mensajesDe(Sesion sesion) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM mensajes WHERE usuario_id = ?", Long.class,
                sesion.usuario().getId());
    }

    @CeladorYPorteria
    void entraASusMensajes(Perfil perfil) throws Exception {
        mvc.perform(con(entrarComo(perfil), get("/api/mensajes"))).andExpect(status().isOk());
    }

    @CeladorYPorteria
    void enviaUnMensaje(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(conJson(sesion, post("/api/mensajes"), Map.of("texto", "Hola"))).andExpect(status().isCreated());
        assertThat(mensajesDe(sesion)).isEqualTo(1);
    }

    @CeladorYPorteria
    void mensajeVacioNoSeEnvia(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(conJson(sesion, post("/api/mensajes"), Map.of("texto", "   ")));
        assertThat(mensajesDe(sesion)).isZero();
    }
}
