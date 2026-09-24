package co.sena.adso.porteria.roles.celador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import co.sena.adso.porteria.soporte.Perfil;

// Portería 2: tests/roles/celador/test_panel.py
class PanelTest extends PruebaRol {

    @CeladorYPorteria
    void entraAlPanel(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/porteria/panel"))).andExpect(status().isOk());
    }

    @CeladorYPorteria
    void exportaElHistorial(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/porteria/panel/exportar").param("desde", "2026-01-01").param("hasta", "2026-12-31")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }
}
