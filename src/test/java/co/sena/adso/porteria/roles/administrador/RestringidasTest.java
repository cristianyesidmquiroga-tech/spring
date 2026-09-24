package co.sena.adso.porteria.roles.administrador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// Portería 2: tests/roles/administrador/test_restringidas.py
class RestringidasTest extends PruebaRol {

    // PROGRAMADAS: ambientes, asistencia, comunicados, fichas e historial_clases (fase 3); historial_cambios y respaldos (fase 5)
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "gestion_usuarios, /api/admin/usuarios",
            "revision_fotos, /api/admin/fotos"
    })
    void noEntra(String vista, String url) throws Exception {
        Sesion sesion = sesion();
        mvc.perform(con(sesion, get(url))).andExpect(status().isForbidden());
    }
}
