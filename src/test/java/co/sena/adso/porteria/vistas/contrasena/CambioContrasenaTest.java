package co.sena.adso.porteria.vistas.contrasena;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

// Portería 2: tests/vistas/cambio_contrasena/test_cambio_contrasena.py
class CambioContrasenaTest extends PruebaIntegracion {

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario cambiar(String actual, String nueva, String confirmacion) throws Exception {
        Sesion sesion = entrarComo(Perfil.APRENDIZ, u -> u.setDebeCambiarContrasena(true));
        mvc.perform(conJson(sesion, post("/api/auth/cambiar-contrasena"),
                Map.of("actual", actual, "nueva", nueva, "confirmacion", confirmacion)));
        return usuarioRepository.findById(sesion.usuario().getId()).orElseThrow();
    }

    // Flask redirigía al inicio; la API responde 403
    @Test
    void sinContrasenaTemporalNoEntra() throws Exception {
        Sesion sesion = entrarComo(Perfil.APRENDIZ);
        mvc.perform(conJson(sesion, post("/api/auth/cambiar-contrasena"),
                        Map.of("actual", "Segura2026", "nueva", "NuevaClave2026", "confirmacion", "NuevaClave2026")))
                .andExpect(status().isForbidden());
    }

    @Test
    void cambiaLaContrasenaTemporal() throws Exception {
        Usuario usuario = cambiar("Segura2026", "NuevaClave2026", "NuevaClave2026");
        assertThat(usuario.isDebeCambiarContrasena()).isFalse();
        assertThat(passwordEncoder.matches("NuevaClave2026", usuario.getContrasena())).isTrue();
    }

    @Test
    void exigeLaContrasenaActual() throws Exception {
        assertThat(cambiar("Incorrecta2026", "NuevaClave2026", "NuevaClave2026").isDebeCambiarContrasena()).isTrue();
    }

    @Test
    void laNuevaDebeSerDistinta() throws Exception {
        assertThat(cambiar("Segura2026", "Segura2026", "Segura2026").isDebeCambiarContrasena()).isTrue();
    }
}
