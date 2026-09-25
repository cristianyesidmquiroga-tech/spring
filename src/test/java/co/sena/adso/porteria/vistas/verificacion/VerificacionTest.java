package co.sena.adso.porteria.vistas.verificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

// Portería 2: tests/vistas/verificacion/test_verificacion.py
class VerificacionTest extends PruebaIntegracion {

    @Autowired
    private Clock reloj;

    private Sesion sinVerificar(LocalDateTime vence) throws Exception {
        return entrarComo(Perfil.APRENDIZ, u -> {
            u.setCorreoVerificado(false);
            u.nuevoCodigoVerificacion("123456", vence);
        });
    }

    private void verificar(Sesion sesion, String codigo) throws Exception {
        mvc.perform(conJson(sesion, post("/api/auth/verificacion"), Map.of("codigo", codigo)));
    }

    private boolean verificado(Sesion sesion) {
        return jdbc.queryForObject("SELECT correo_verificado FROM usuarios WHERE id = ?", Boolean.class, sesion.usuario().getId());
    }

    @Test
    void yaVerificadoNoEntra() throws Exception {
        mvc.perform(conJson(entrarComo(Perfil.APRENDIZ), post("/api/auth/verificacion"), Map.of("codigo", "123456")))
                .andExpect(status().isConflict());
    }

    @Test
    void verificaConElCodigoCorrecto() throws Exception {
        Sesion sesion = sinVerificar(LocalDateTime.now(reloj).plusMinutes(10));
        verificar(sesion, "123456");
        assertThat(verificado(sesion)).isTrue();
    }

    @Test
    void codigoIncorrectoNoVerifica() throws Exception {
        Sesion sesion = sinVerificar(LocalDateTime.now(reloj).plusMinutes(10));
        verificar(sesion, "000000");
        assertThat(verificado(sesion)).isFalse();
    }

    @Test
    void codigoExpiradoNoVerifica() throws Exception {
        Sesion sesion = sinVerificar(LocalDateTime.now(reloj).minusMinutes(1));
        verificar(sesion, "123456");
        assertThat(verificado(sesion)).isFalse();
    }
}
