package co.sena.adso.porteria.roles.instructor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// Portería 2: tests/roles/instructor/test_restringidas.py
class RestringidasTest extends PruebaRol {

    // PROGRAMADAS: ambientes, fichas e historial_clases (fase 3); bandeja_mensajes (fase 4); historial_cambios y respaldos (fase 5)
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "escaner, /api/porteria/verificar?codigo=999",
            "gestion_usuarios, /api/admin/usuarios",
            "panel, /api/porteria/panel",
            "pases, /api/porteria/pases",
            "reportes, /api/porteria/panel/reportes/Aprendiz",
            "revision_fotos, /api/admin/fotos"
    })
    void noEntra(String vista, String url) throws Exception {
        Sesion sesion = sesion();
        mvc.perform(con(sesion, get(url))).andExpect(status().isForbidden());
    }
}
