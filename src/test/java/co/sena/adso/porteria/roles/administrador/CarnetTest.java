package co.sena.adso.porteria.roles.administrador;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import co.sena.adso.porteria.roles.PruebaRol;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/administrador/test_carnet.py
class CarnetTest extends PruebaRol {

    @Test
    void elCarnetMuestraSuPerfil() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(con(sesion, get("/api/perfil/carnet"))).andExpect(jsonPath("$.perfil").value("FUNCIONARIO"));
    }

    @Test
    void elCarnetMuestraSuDocumento() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(con(sesion, get("/api/perfil/carnet")))
                .andExpect(jsonPath("$.documento").value(containsString(sesion.usuario().getDocumento())));
    }
}
