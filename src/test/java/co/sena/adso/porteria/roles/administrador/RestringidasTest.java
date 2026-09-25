package co.sena.adso.porteria.roles.administrador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// Portería 2: tests/roles/administrador/test_restringidas.py
class RestringidasTest extends PruebaRol {

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "ambientes, /api/ambientes",
            "asistencia, /api/asistencia",
            "comunicados, /api/comunicados",
            "fichas, /api/admin/fichas",
            "gestion_usuarios, /api/admin/usuarios",
            "historial_cambios, /api/admin/auditoria",
            "historial_clases, /api/admin/clases",
            "respaldos, /api/admin/respaldos",
            "revision_fotos, /api/admin/fotos"
    })
    void noEntra(String vista, String url) throws Exception {
        Sesion sesion = sesion();
        mvc.perform(con(sesion, get(url))).andExpect(status().isForbidden());
    }
}
