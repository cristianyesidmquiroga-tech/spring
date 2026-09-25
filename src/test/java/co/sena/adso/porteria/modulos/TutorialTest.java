package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// Portería 2: tests/modulos/test_tutorial.py
class TutorialTest extends PruebaIntegracion {

    private Sesion ana() throws Exception {
        Usuario u = crearUsuario("ana@sena.edu.co", "111");
        return new Sesion(u, iniciarSesion(u.getCorreo(), CLAVE));
    }

    private boolean visto(Usuario u) {
        return jdbc.queryForObject("SELECT tutorial_visto FROM usuarios WHERE id = ?", Boolean.class, u.getId());
    }

    @Nested
    class MarcarComoVisto {

        @Test
        void seMarcaElTutorialComoVisto() throws Exception {
            Sesion ana = ana();
            assertThat(visto(ana.usuario())).isFalse();
            mvc.perform(con(ana, post("/api/tutorial/completar"))).andExpect(status().isOk());
            assertThat(visto(ana.usuario())).isTrue();
        }

        @Test
        void marcarloDosVecesNoFalla() throws Exception {
            Sesion ana = ana();
            mvc.perform(con(ana, post("/api/tutorial/completar")));
            mvc.perform(con(ana, post("/api/tutorial/completar"))).andExpect(status().isOk());
            assertThat(visto(ana.usuario())).isTrue();
        }

        @Test
        void sinSesionNoSePuedeMarcar() throws Exception {
            mvc.perform(post("/api/tutorial/completar")).andExpect(status().isUnauthorized());
        }

        // Opera sobre quien tiene la sesión: un id ajeno en el cuerpo no toca a la otra persona
        @Test
        void noSePuedeMarcarElDeOtraPersona() throws Exception {
            Usuario ana = crearUsuario("ana@sena.edu.co", "111");
            Usuario beto = crearUsuario("beto@sena.edu.co", "222");
            Sesion sesionBeto = new Sesion(beto, iniciarSesion(beto.getCorreo(), CLAVE));
            mvc.perform(conJson(sesionBeto, post("/api/tutorial/completar"), Map.of("usuarioId", ana.getId())));
            mvc.perform(con(sesionBeto, post("/api/tutorial/completar").param("id", ana.getId().toString())));
            assertThat(visto(ana)).isFalse();
            assertThat(visto(beto)).isTrue();
        }
    }

    @Nested
    class VersionEnTexto {

        @Test
        void laPaginaDelTutorialCarga() throws Exception {
            String cuerpo = mvc.perform(con(ana(), get("/api/tutorial"))).andExpect(status().isOk()).andReturn()
                    .getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(cuerpo).contains("Completa tu información personal", "Sube tu foto de perfil",
                    "aprobación de un asesor", "Ejemplo:");
        }

        @Test
        void sinSesionRedirigeAlLogin() throws Exception {
            mvc.perform(get("/api/tutorial")).andExpect(status().isUnauthorized());
        }

        // En React el enlace ?tutorial=1 relanza el recorrido; la API sigue entregando los pasos aunque ya se haya visto
        @Test
        void ofreceRelanzarElRecorridoGuiado() throws Exception {
            Sesion ana = ana();
            mvc.perform(con(ana, post("/api/tutorial/completar")));
            mvc.perform(con(ana, get("/api/tutorial")))
                    .andExpect(jsonPath("$.visto").value(true))
                    .andExpect(jsonPath("$.pasos.length()").value(6));
        }
    }

    // El botón "Ver tutorial de primeros pasos" lo pinta React mientras el perfil esté incompleto
    @Nested
    class BotonDeAcceso {

        @Test
        void apareceConElPerfilIncompleto() throws Exception {
            Usuario u = crearUsuario("ana@sena.edu.co", "Aprendiz", "Usuario", "111", x -> x.setPerfilCompleto(false));
            mvc.perform(con(new Sesion(u, iniciarSesion(u.getCorreo(), CLAVE)), get("/api/perfil")))
                    .andExpect(jsonPath("$.perfilCompleto").value(false));
        }

        @Test
        void desapareceConElPerfilCompleto() throws Exception {
            mvc.perform(con(ana(), get("/api/perfil"))).andExpect(jsonPath("$.perfilCompleto").value(true));
        }
    }

    // El recorrido se lanza solo según tutorialVisto de la sesión
    @Nested
    class RecorridoGuiado {

        @Test
        void elScriptIndicaQueNoSeHaVisto() throws Exception {
            mvc.perform(con(ana(), get("/api/auth/yo"))).andExpect(jsonPath("$.tutorialVisto").value(false));
        }

        @Test
        void elScriptIndicaQueYaSeVio() throws Exception {
            Sesion ana = ana();
            jdbc.update("UPDATE usuarios SET tutorial_visto = true WHERE id = ?", ana.usuario().getId());
            mvc.perform(con(ana, get("/api/auth/yo"))).andExpect(jsonPath("$.tutorialVisto").value(true));
        }
    }
}
