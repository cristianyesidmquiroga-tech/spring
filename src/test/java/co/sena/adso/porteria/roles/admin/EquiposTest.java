package co.sena.adso.porteria.roles.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import java.util.Map;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/admin/test_equipos.py
class EquiposTest extends PruebaRol {

    @Test
    void registraUnEquipo() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(conJson(sesion, post("/api/equipos"),
                        Map.of("nombre", "Portatil HP", "serial", "SN-ROL-1", "tipo", "Portátil")))
                .andExpect(status().isCreated());
        assertThat(equipos(sesion)).isEqualTo(1);
    }

    @Test
    void eliminaSuEquipo() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(conJson(sesion, post("/api/equipos"), Map.of("nombre", "Tablet", "tipo", "Tablet")));
        Long equipo = jdbc.queryForObject("SELECT id FROM equipos WHERE usuario_id = ?", Long.class, sesion.usuario().getId());
        mvc.perform(con(sesion, delete("/api/equipos/{id}", equipo))).andExpect(status().isNoContent());
        assertThat(equipos(sesion)).isZero();
    }

    private Integer equipos(Sesion sesion) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM equipos WHERE usuario_id = ?", Integer.class, sesion.usuario().getId());
    }
}
