package co.sena.adso.porteria.roles.coordinacion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/coordinacion/test_ambientes.py
class AmbientesTest extends PruebaRol {

    @Test
    void entraAAmbientes() throws Exception {
        mvc.perform(con(sesion(), get("/api/ambientes"))).andExpect(status().isOk());
    }

    @Test
    void veElDetalleDeUnAmbiente() throws Exception {
        mvc.perform(con(sesion(), get("/api/ambientes/2758291"))).andExpect(status().isOk());
    }
}
