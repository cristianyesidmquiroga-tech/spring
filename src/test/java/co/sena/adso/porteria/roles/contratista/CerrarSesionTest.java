package co.sena.adso.porteria.roles.contratista;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/contratista/test_cerrar_sesion.py
class CerrarSesionTest extends PruebaRol {

    @Test
    void cierraSesion() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(con(sesion, post("/api/auth/logout"))).andExpect(status().isNoContent());
    }

    @Test
    void despuesDeSalirNoEntraAlPerfil() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(con(sesion, post("/api/auth/logout")));
        mvc.perform(con(sesion, get("/api/perfil"))).andExpect(status().isUnauthorized());
    }
}
