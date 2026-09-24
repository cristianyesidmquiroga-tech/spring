package co.sena.adso.porteria.vistas.sesion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/cerrar_sesion/test_cerrar_sesion.py
class CerrarSesionTest extends PruebaIntegracion {

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void todosLosPerfilesCierranSesion(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, post("/api/auth/logout"))).andExpect(status().isNoContent());
        mvc.perform(con(sesion, get("/api/perfil"))).andExpect(status().isUnauthorized());
    }

    @Test
    void borraElTokenDeSesion() throws Exception {
        Sesion sesion = entrarComo(Perfil.APRENDIZ);
        mvc.perform(con(sesion, post("/api/auth/logout")));
        assertThat(usuarioRepository.findById(sesion.usuario().getId()).orElseThrow().getSessionToken()).isNull();
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
    }
}
