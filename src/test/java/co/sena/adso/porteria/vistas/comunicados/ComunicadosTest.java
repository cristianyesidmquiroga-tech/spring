package co.sena.adso.porteria.vistas.comunicados;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/comunicados/test_comunicados.py
class ComunicadosTest extends PruebaIntegracion {

    private static final Set<Perfil> ENTRAN = EnumSet.of(Perfil.ADMIN, Perfil.INSTRUCTOR);

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void accesoSegunElPerfil(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/comunicados")))
                .andExpect(ENTRAN.contains(perfil) ? status().isOk() : status().isForbidden());
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(get("/api/comunicados")).andExpect(status().isUnauthorized());
    }

    @Test
    void enviarSinPermisoDevuelve403() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        mvc.perform(conJson(aprendiz, post("/api/comunicados"),
                Map.of("tipo", "Comunicado General", "destinatarios", List.of(1)))).andExpect(status().isForbidden());
    }

    @Test
    void enviarSinDestinatariosSeRechaza() throws Exception {
        Sesion instructor = entrarComo(Perfil.INSTRUCTOR);
        mvc.perform(conJson(instructor, post("/api/comunicados"),
                Map.of("tipo", "Comunicado General", "destinatarios", List.of()))).andExpect(status().isBadRequest());
    }
}
