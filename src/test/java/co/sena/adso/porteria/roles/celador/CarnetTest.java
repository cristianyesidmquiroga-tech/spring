package co.sena.adso.porteria.roles.celador;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import co.sena.adso.porteria.roles.PruebaRol;
import co.sena.adso.porteria.soporte.Perfil;

// Portería 2: tests/roles/celador/test_carnet.py
class CarnetTest extends PruebaRol {

    @CeladorYPorteria
    void elCarnetMuestraSuPerfil(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/perfil/carnet"))).andExpect(jsonPath("$.perfil").value("CONTRATISTA"));
    }

    @CeladorYPorteria
    void elCarnetMuestraSuDocumento(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/perfil/carnet")))
                .andExpect(jsonPath("$.documento").value(containsString(sesion.usuario().getDocumento())));
    }
}
