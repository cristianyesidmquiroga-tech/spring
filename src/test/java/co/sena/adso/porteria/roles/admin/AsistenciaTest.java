package co.sena.adso.porteria.roles.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/admin/test_asistencia.py
class AsistenciaTest extends PruebaRol {

    @Test
    void entraAAsistencia() throws Exception {
        mvc.perform(con(sesion(), get("/api/asistencia"))).andExpect(status().isOk());
    }

    @Test
    void buscaUnaFicha() throws Exception {
        mvc.perform(con(sesion(), get("/api/asistencia").param("ficha", "2758291"))).andExpect(status().isOk());
    }
}
