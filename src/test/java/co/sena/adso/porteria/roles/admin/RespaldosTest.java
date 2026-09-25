package co.sena.adso.porteria.roles.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/admin/test_respaldos.py
class RespaldosTest extends PruebaRol {

    @Test
    void entraARespaldos() throws Exception {
        mvc.perform(con(sesion(), get("/api/admin/respaldos"))).andExpect(status().isOk());
    }
}
