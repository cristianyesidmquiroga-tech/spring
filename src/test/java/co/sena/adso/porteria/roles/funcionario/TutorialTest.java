package co.sena.adso.porteria.roles.funcionario;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/funcionario/test_tutorial.py
class TutorialTest extends PruebaRol {

    @Test
    void entraAlTutorial() throws Exception {
        mvc.perform(con(sesion(), get("/api/tutorial"))).andExpect(status().isOk());
    }

    @Test
    void marcaElTutorialComoVisto() throws Exception {
        mvc.perform(con(sesion(), post("/api/tutorial/completar"))).andExpect(status().isOk());
    }
}
