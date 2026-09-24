package co.sena.adso.porteria.roles.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/admin/test_panel.py
class PanelTest extends PruebaRol {

    @Test
    void entraAlPanel() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(con(sesion, get("/api/porteria/panel"))).andExpect(status().isOk());
    }

    @Test
    void exportaElHistorial() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(con(sesion, get("/api/porteria/panel/exportar").param("desde", "2026-01-01").param("hasta", "2026-12-31")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }
}
