package co.sena.adso.porteria.roles.celador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.roles.PruebaRol;
import co.sena.adso.porteria.soporte.Perfil;

// Portería 2: tests/roles/celador/test_historial_ingresos.py
class HistorialIngresosTest extends PruebaRol {

    @CeladorYPorteria
    void entraASuHistorial(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/historial"))).andExpect(status().isOk());
    }

    @CeladorYPorteria
    void consultaSuPropioHistorial(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/historial").param("usuarioId", String.valueOf(sesion.usuario().getId()))))
                .andExpect(status().isOk());
    }

    @CeladorYPorteria
    void consultaElHistorialDeOtraPersona(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        Usuario otro = otroUsuario();
        mvc.perform(con(sesion, get("/api/historial").param("usuarioId", String.valueOf(otro.getId()))))
                .andExpect(status().isOk());
    }
}
