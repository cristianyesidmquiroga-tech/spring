package co.sena.adso.porteria.roles.administrador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// Portería 2: tests/roles/administrador/test_reportes.py
class ReportesTest extends PruebaRol {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"Aprendiz", "Instructor", "Personal"})
    void entraAReportes(String cargo) throws Exception {
        Sesion sesion = sesion();
        mvc.perform(con(sesion, get("/api/porteria/panel/reportes/{cargo}", cargo))).andExpect(status().isOk());
    }
}
