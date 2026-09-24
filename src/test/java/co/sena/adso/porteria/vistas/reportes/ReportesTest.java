package co.sena.adso.porteria.vistas.reportes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

// Portería 2: tests/vistas/reportes/test_reportes.py
class ReportesTest extends PruebaIntegracion {

    private static final Set<Perfil> ENTRAN = EnumSet.of(Perfil.ADMIN, Perfil.ADMINISTRADOR, Perfil.CELADOR, Perfil.PORTERIA);

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void accesoSegunElPerfil(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/porteria/panel/reportes/Aprendiz")))
                .andExpect(ENTRAN.contains(perfil) ? status().isOk() : status().isForbidden());
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(get("/api/porteria/panel/reportes/Aprendiz")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"Aprendiz", "Instructor", "Personal"})
    void cadaReporteAbre(String cargo) throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        mvc.perform(con(admin, get("/api/porteria/panel/reportes/" + cargo))).andExpect(status().isOk());
    }
}
