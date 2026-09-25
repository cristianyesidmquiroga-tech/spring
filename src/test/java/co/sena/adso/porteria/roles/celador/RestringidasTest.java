package co.sena.adso.porteria.roles.celador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import co.sena.adso.porteria.soporte.Perfil;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// Portería 2: tests/roles/celador/test_restringidas.py
class RestringidasTest extends PruebaRol {

    // PROGRAMADAS: historial_cambios y respaldos (fase 5)
    static Stream<Arguments> vistas() {
        return Stream.of(Perfil.CELADOR, Perfil.PORTERIA).flatMap(p -> Stream.of(
                Arguments.of(p, "bandeja_mensajes", "/api/bandeja"),
                Arguments.of(p, "ambientes", "/api/ambientes"),
                Arguments.of(p, "asistencia", "/api/asistencia"),
                Arguments.of(p, "comunicados", "/api/comunicados"),
                Arguments.of(p, "fichas", "/api/admin/fichas"),
                Arguments.of(p, "historial_clases", "/api/admin/clases"),
                Arguments.of(p, "gestion_usuarios", "/api/admin/usuarios"),
                Arguments.of(p, "revision_fotos", "/api/admin/fotos")));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("vistas")
    void noEntra(Perfil perfil, String vista, String url) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get(url))).andExpect(status().isForbidden());
    }

    // Portería 2 respondía 400 al negar el permiso; Spring responde 403
    @CeladorYPorteria
    void noRegistraEquipos(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(conJson(sesion, post("/api/equipos"), Map.of("nombre", "Portatil", "tipo", "Otro")))
                .andExpect(status().isForbidden());
    }
}
