package co.sena.adso.porteria.roles.administrador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import java.util.Map;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/administrador/test_pases.py
class PasesTest extends PruebaRol {

    @Test
    void entraAPases() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(con(sesion, get("/api/porteria/pases"))).andExpect(status().isOk());
    }

    @Test
    void registraUnVisitante() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(conJson(sesion, post("/api/porteria/pases/visitantes"),
                Map.of("nombre", "Visitante", "documento", "2000000001", "motivo", "Reunion")));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM visitantes WHERE documento = '2000000001'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void registraUnVehiculo() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(conJson(sesion, post("/api/porteria/pases/vehiculos"), Map.of("placa", "abc123", "tipo", "Externo")));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vehiculos WHERE placa = 'ABC123'", Integer.class))
                .isEqualTo(1);
    }
}
