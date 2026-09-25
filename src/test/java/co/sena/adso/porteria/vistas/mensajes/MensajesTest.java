package co.sena.adso.porteria.vistas.mensajes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/mensajes/test_mensajes.py
class MensajesTest extends PruebaIntegracion {

    private long mensajesDe(Usuario u) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM mensajes WHERE usuario_id = ?", Long.class, u.getId());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void todosLosPerfilesEntran(Perfil perfil) throws Exception {
        mvc.perform(con(entrarComo(perfil), get("/api/mensajes"))).andExpect(status().isOk());
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(get("/api/mensajes")).andExpect(status().isUnauthorized());
    }

    @Test
    void enviaUnMensaje() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        mvc.perform(conJson(aprendiz, post("/api/mensajes"), Map.of("texto", "Hola")));
        assertThat(mensajesDe(aprendiz.usuario())).isEqualTo(1);
    }

    @Test
    void mensajeDemasiadoLargoNoSeEnvia() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        mvc.perform(conJson(aprendiz, post("/api/mensajes"), Map.of("texto", "a".repeat(2001))))
                .andExpect(status().isBadRequest());
        assertThat(mensajesDe(aprendiz.usuario())).isZero();
    }

    @Test
    void soloVeSusMensajes() throws Exception {
        Usuario otro = crearUsuario("otra.persona@sena.edu.co", "3000000002");
        jdbc.update("INSERT INTO mensajes (usuario_id, autor_id, autor_nombre, texto) VALUES (?, ?, ?, 'Mensaje privado')",
                otro.getId(), otro.getId(), otro.getNombre());
        String cuerpo = mvc.perform(con(entrarComo(Perfil.APRENDIZ), get("/api/mensajes"))).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(cuerpo).doesNotContain("Mensaje privado");
    }
}
