package co.sena.adso.porteria.vistas.asistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/asistencia/test_asistencia.py
class AsistenciaTest extends PruebaIntegracion {

    private static final Set<Perfil> ENTRAN = EnumSet.of(Perfil.ADMIN, Perfil.INSTRUCTOR);

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void accesoSegunElPerfil(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/asistencia")))
                .andExpect(ENTRAN.contains(perfil) ? status().isOk() : status().isForbidden());
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(get("/api/asistencia")).andExpect(status().isUnauthorized());
    }

    @Test
    void guardaLaAsistenciaDeLaFicha() throws Exception {
        Usuario aprendiz = aprendizDeFicha();
        registrarEntradaHoy(aprendiz);
        Sesion instructor = entrarComo(Perfil.INSTRUCTOR);
        mvc.perform(conJson(instructor, post("/api/asistencia"),
                Map.of("ficha", "2758291", "presentes", List.of(aprendiz.getId())))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT presente FROM asistencia_clases WHERE aprendiz_id = ?", Boolean.class,
                aprendiz.getId())).isTrue();
    }
}
