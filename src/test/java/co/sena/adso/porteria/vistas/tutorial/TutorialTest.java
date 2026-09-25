package co.sena.adso.porteria.vistas.tutorial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/tutorial/test_tutorial.py
class TutorialTest extends PruebaIntegracion {

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void todosLosPerfilesEntran(Perfil perfil) throws Exception {
        mvc.perform(con(entrarComo(perfil), get("/api/tutorial"))).andExpect(status().isOk());
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(get("/api/tutorial")).andExpect(status().isUnauthorized());
    }

    @Test
    void marcaElTutorialComoVisto() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        mvc.perform(con(aprendiz, post("/api/tutorial/completar")));
        assertThat(jdbc.queryForObject("SELECT tutorial_visto FROM usuarios WHERE id = ?", Boolean.class,
                aprendiz.usuario().getId())).isTrue();
    }
}
