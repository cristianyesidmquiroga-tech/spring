package co.sena.adso.porteria.roles.administrativo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.roles.PruebaRol;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/administrativo/test_historial_ingresos.py
class HistorialIngresosTest extends PruebaRol {

    @Test
    void entraASuHistorial() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(con(sesion, get("/api/historial"))).andExpect(status().isOk());
    }

    @Test
    void consultaSuPropioHistorial() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(con(sesion, get("/api/historial").param("usuarioId", String.valueOf(sesion.usuario().getId()))))
                .andExpect(status().isOk());
    }

    @Test
    void noConsultaElHistorialDeOtraPersona() throws Exception {
        Sesion sesion = sesion();
        Usuario otro = otroUsuario();
        mvc.perform(con(sesion, get("/api/historial").param("usuarioId", String.valueOf(otro.getId()))))
                .andExpect(status().isForbidden());
    }
}
