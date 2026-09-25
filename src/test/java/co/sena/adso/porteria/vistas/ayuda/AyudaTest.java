package co.sena.adso.porteria.vistas.ayuda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/ayuda/test_ayuda.py
class AyudaTest extends PruebaIntegracion {

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void todosLosPerfilesEntran(Perfil perfil) throws Exception {
        mvc.perform(con(entrarComo(perfil), get("/api/ayuda"))).andExpect(status().isOk());
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(get("/api/ayuda")).andExpect(status().isUnauthorized());
    }

    @Test
    void contactoConAsuntoLlegaAMensajes() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        mvc.perform(conJson(aprendiz, post("/api/ayuda/contacto"),
                Map.of("asunto", "Problema con mi foto de perfil", "detalle", "No carga")));
        String texto = jdbc.queryForObject("SELECT texto FROM mensajes WHERE usuario_id = ?", String.class,
                aprendiz.usuario().getId());
        assertThat(texto).startsWith("[Problema con mi foto de perfil]");
    }

    @Test
    void contactoVacioNoSeEnvia() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        mvc.perform(conJson(aprendiz, post("/api/ayuda/contacto"), Map.of("asunto", "Otro", "detalle", "")));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM mensajes WHERE usuario_id = ?", Long.class,
                aprendiz.usuario().getId())).isZero();
    }
}
