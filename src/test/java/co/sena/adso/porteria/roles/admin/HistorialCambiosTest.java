package co.sena.adso.porteria.roles.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/admin/test_historial_cambios.py
class HistorialCambiosTest extends PruebaRol {

    @Test
    void entraAlHistorialDeCambios() throws Exception {
        mvc.perform(con(sesion(), get("/api/admin/auditoria"))).andExpect(status().isOk());
    }
}
