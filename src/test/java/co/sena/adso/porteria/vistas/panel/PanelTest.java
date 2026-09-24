package co.sena.adso.porteria.vistas.panel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

// Portería 2: tests/vistas/panel/test_panel.py
class PanelTest extends PruebaIntegracion {

    private static final Set<Perfil> ENTRAN = EnumSet.of(Perfil.ADMIN, Perfil.ADMINISTRADOR, Perfil.CELADOR, Perfil.PORTERIA);

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void accesoSegunElPerfil(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/porteria/panel")))
                .andExpect(ENTRAN.contains(perfil) ? status().isOk() : status().isForbidden());
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(get("/api/porteria/panel")).andExpect(status().isUnauthorized());
    }

    @Test
    void exportarSinFechasNoDescarga() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        mvc.perform(con(admin, get("/api/porteria/panel/exportar"))).andExpect(status().isBadRequest());
    }

    @Test
    void exportarConFechasDescargaCsv() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        MvcResult r = mvc.perform(con(celador, get("/api/porteria/panel/exportar")
                .param("desde", "2026-01-01").param("hasta", "2026-12-31"))).andReturn();
        assertThat(MediaType.parseMediaType(r.getResponse().getContentType()).isCompatibleWith(new MediaType("text", "csv")))
                .isTrue();
    }

    @Test
    void exportarSinPermisoNoDescarga() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        mvc.perform(con(aprendiz, get("/api/porteria/panel/exportar")
                .param("desde", "2026-01-01").param("hasta", "2026-12-31"))).andExpect(status().isForbidden());
    }
}
