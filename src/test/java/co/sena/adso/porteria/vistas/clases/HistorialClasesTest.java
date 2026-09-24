package co.sena.adso.porteria.vistas.clases;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/historial_clases/test_historial_clases.py
class HistorialClasesTest extends PruebaIntegracion {

    private static final String RUTA = "/api/admin/clases";

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void accesoSegunElPerfil(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get(RUTA)))
                .andExpect(perfil == Perfil.ADMIN ? status().isOk() : status().isForbidden());
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    }

    @Test
    void buscaPorFicha() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        Usuario aprendiz = aprendizDeFicha();
        jdbc.update("INSERT INTO asistencia_clases (instructor_id, aprendiz_id, ficha, presente) "
                + "VALUES (?, ?, '2758291', true)", admin.usuario().getId(), aprendiz.getId());
        mvc.perform(con(admin, get(RUTA).param("ficha", "2758291")))
                .andExpect(jsonPath("$[0].aprendiz").value("Aprendiz Buscado"));
    }
}
