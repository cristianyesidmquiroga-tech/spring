package co.sena.adso.porteria.roles.celador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import co.sena.adso.porteria.soporte.Perfil;
import java.util.Map;

// Portería 2: tests/roles/celador/test_pases.py
class PasesTest extends PruebaRol {

    @CeladorYPorteria
    void entraAPases(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/porteria/pases"))).andExpect(status().isOk());
    }

    @CeladorYPorteria
    void registraUnVisitante(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(conJson(sesion, post("/api/porteria/pases/visitantes"),
                Map.of("nombre", "Visitante", "documento", "2000000001", "motivo", "Reunion")));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM visitantes WHERE documento = '2000000001'", Integer.class))
                .isEqualTo(1);
    }

    @CeladorYPorteria
    void registraUnVehiculo(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(conJson(sesion, post("/api/porteria/pases/vehiculos"), Map.of("placa", "abc123", "tipo", "Externo")));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vehiculos WHERE placa = 'ABC123'", Integer.class))
                .isEqualTo(1);
    }
}
