package co.sena.adso.porteria.roles.celador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import co.sena.adso.porteria.soporte.Perfil;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// Portería 2: tests/roles/celador/test_reportes.py
class ReportesTest extends PruebaRol {

    static Stream<Arguments> casos() {
        return Stream.of(Perfil.CELADOR, Perfil.PORTERIA).flatMap(p -> Stream.of("Aprendiz", "Instructor", "Personal")
                .map(cargo -> Arguments.of(p, cargo)));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("casos")
    void entraAReportes(Perfil perfil, String cargo) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/porteria/panel/reportes/{cargo}", cargo))).andExpect(status().isOk());
    }
}
