package co.sena.adso.porteria.vistas.ingresos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/historial_ingresos/test_historial_ingresos.py
class HistorialIngresosTest extends PruebaIntegracion {

    private static final Set<Perfil> TERCEROS = EnumSet.of(Perfil.ADMIN, Perfil.ADMINISTRADOR, Perfil.CELADOR,
            Perfil.INSTRUCTOR, Perfil.PORTERIA);

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void todosLosPerfilesEntran(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/historial"))).andExpect(status().isOk());
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(get("/api/historial")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void historialDeOtraPersonaSegunElPerfil(Perfil perfil) throws Exception {
        Usuario otro = crearUsuario("otra.persona@sena.edu.co", "3000000002");
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/historial").param("usuarioId", otro.getId().toString())))
                .andExpect(TERCEROS.contains(perfil) ? status().isOk() : status().isForbidden());
    }

    @Test
    void suPropioHistorial() throws Exception {
        Sesion sesion = entrarComo(Perfil.APRENDIZ);
        mvc.perform(con(sesion, get("/api/historial").param("usuarioId", sesion.usuario().getId().toString())))
                .andExpect(status().isOk());
    }
}
