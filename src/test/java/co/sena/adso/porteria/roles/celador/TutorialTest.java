package co.sena.adso.porteria.roles.celador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import co.sena.adso.porteria.soporte.Perfil;

// Portería 2: tests/roles/celador/test_tutorial.py
class TutorialTest extends PruebaRol {

    @CeladorYPorteria
    void entraAlTutorial(Perfil perfil) throws Exception {
        mvc.perform(con(entrarComo(perfil), get("/api/tutorial"))).andExpect(status().isOk());
    }

    @CeladorYPorteria
    void marcaElTutorialComoVisto(Perfil perfil) throws Exception {
        mvc.perform(con(entrarComo(perfil), post("/api/tutorial/completar"))).andExpect(status().isOk());
    }
}
