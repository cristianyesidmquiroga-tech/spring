package co.sena.adso.porteria.vistas.bandeja;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/bandeja_mensajes/test_bandeja_mensajes.py
class BandejaMensajesTest extends PruebaIntegracion {

    private static final Set<Perfil> ENTRAN = EnumSet.of(Perfil.ADMIN, Perfil.ADMINISTRADOR, Perfil.ADMINISTRATIVO);

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void accesoSegunElPerfil(Perfil perfil) throws Exception {
        mvc.perform(con(entrarComo(perfil), get("/api/bandeja")))
                .andExpect(ENTRAN.contains(perfil) ? status().isOk() : status().isForbidden());
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(get("/api/bandeja")).andExpect(status().isUnauthorized());
    }

    @Test
    void responderSinPermisoDevuelve403() throws Exception {
        Usuario otro = crearUsuario("otra.persona@sena.edu.co", "3000000002");
        mvc.perform(conJson(entrarComo(Perfil.APRENDIZ), post("/api/bandeja/{id}", otro.getId()), Map.of("texto", "Hola")))
                .andExpect(status().isForbidden());
    }

    @Test
    void responderAQuienNoExisteDevuelve404() throws Exception {
        mvc.perform(conJson(entrarComo(Perfil.ADMIN), post("/api/bandeja/99999"), Map.of("texto", "Hola")))
                .andExpect(status().isNotFound());
    }

    @Test
    void elDocumentoSeVeEnmascarado() throws Exception {
        Usuario otro = crearUsuario("otra.persona@sena.edu.co", "3000000002");
        String cuerpo = mvc.perform(con(entrarComo(Perfil.ADMINISTRATIVO), get("/api/bandeja/{id}", otro.getId())))
                .andReturn().getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain("3000000002");
    }
}
