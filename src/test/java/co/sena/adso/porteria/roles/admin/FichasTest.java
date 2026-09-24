package co.sena.adso.porteria.roles.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import java.util.Map;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/admin/test_fichas.py
class FichasTest extends PruebaRol {

    @Test
    void entraAFichas() throws Exception {
        mvc.perform(con(sesion(), get("/api/admin/fichas"))).andExpect(status().isOk());
    }

    @Test
    void creaUnaFicha() throws Exception {
        mvc.perform(conJson(sesion(), post("/api/admin/fichas"),
                Map.of("numero", "2758291", "programa", "Analisis y Desarrollo")));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM fichas WHERE numero = '2758291'", Long.class)).isEqualTo(1);
    }
}
