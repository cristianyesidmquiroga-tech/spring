package co.sena.adso.porteria.vistas.carnet;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/carnet/test_carnet.py
class CarnetTest extends PruebaIntegracion {

    private static final Map<Perfil, String> PERFIL_CARNET = Map.ofEntries(
            Map.entry(Perfil.ADMIN, "FUNCIONARIO"), Map.entry(Perfil.ADMINISTRADOR, "FUNCIONARIO"),
            Map.entry(Perfil.ADMINISTRATIVO, "FUNCIONARIO"), Map.entry(Perfil.APRENDIZ, "APRENDIZ"),
            Map.entry(Perfil.CELADOR, "CONTRATISTA"), Map.entry(Perfil.CONTRATISTA, "CONTRATISTA"),
            Map.entry(Perfil.COORDINACION, "FUNCIONARIO"), Map.entry(Perfil.FUNCIONARIO, "FUNCIONARIO"),
            Map.entry(Perfil.INSTRUCTOR, "INSTRUCTOR"), Map.entry(Perfil.PORTERIA, "CONTRATISTA"),
            Map.entry(Perfil.SUBDIRECTOR, "SUBDIRECTOR"), Map.entry(Perfil.TRABAJADOR, "FUNCIONARIO"));

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void cadaPerfilVeSuCarnet(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/perfil/carnet"))).andExpect(jsonPath("$.perfil").value(PERFIL_CARNET.get(perfil)));
    }

    @Test
    void conPerfilCompletoMuestraCodigoDeBarras() throws Exception {
        Sesion sesion = entrarComo(Perfil.APRENDIZ);
        mvc.perform(con(sesion, get("/api/perfil/carnet"))).andExpect(jsonPath("$.codigoBarras").isNotEmpty());
    }

    @Test
    void conPerfilIncompletoNoMuestraCodigoDeBarras() throws Exception {
        Sesion sesion = entrarComo(Perfil.APRENDIZ, u -> u.setPerfilCompleto(false));
        mvc.perform(con(sesion, get("/api/perfil/carnet"))).andExpect(jsonPath("$.codigoBarras").value(nullValue()));
    }
}
