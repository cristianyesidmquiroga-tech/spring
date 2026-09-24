package co.sena.adso.porteria.vistas.ambientes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/ambientes/test_ambientes.py
class AmbientesTest extends PruebaIntegracion {

    private static final Set<Perfil> ENTRAN = EnumSet.of(Perfil.ADMIN, Perfil.COORDINACION, Perfil.SUBDIRECTOR);

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void accesoSegunElPerfil(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/ambientes")))
                .andExpect(ENTRAN.contains(perfil) ? status().isOk() : status().isForbidden());
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(get("/api/ambientes")).andExpect(status().isUnauthorized());
    }

    @Test
    void muestraLaFichaConAprendicesAdentro() throws Exception {
        registrarEntradaHoy(aprendizDeFicha());
        Sesion coordinacion = entrarComo(Perfil.COORDINACION);
        mvc.perform(con(coordinacion, get("/api/ambientes")))
                .andExpect(jsonPath("$[0].ficha").value("2758291"))
                .andExpect(jsonPath("$[0].aprendices").value(1));
    }

    @Test
    void detalleDeUnaFicha() throws Exception {
        Sesion subdirector = entrarComo(Perfil.SUBDIRECTOR);
        mvc.perform(con(subdirector, get("/api/ambientes/2758291"))).andExpect(status().isOk());
    }
}
