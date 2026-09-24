package co.sena.adso.porteria.roles.celador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import co.sena.adso.porteria.soporte.Perfil;

// Portería 2: tests/roles/celador/test_cerrar_sesion.py
class CerrarSesionTest extends PruebaRol {

    @CeladorYPorteria
    void cierraSesion(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, post("/api/auth/logout"))).andExpect(status().isNoContent());
    }

    @CeladorYPorteria
    void despuesDeSalirNoEntraAlPerfil(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, post("/api/auth/logout")));
        mvc.perform(con(sesion, get("/api/perfil"))).andExpect(status().isUnauthorized());
    }
}
