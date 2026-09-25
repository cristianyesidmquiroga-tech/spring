package co.sena.adso.porteria.vistas.recuperacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

// Portería 2: tests/vistas/recuperacion/test_recuperacion.py
class RecuperacionTest extends PruebaIntegracion {

    @Autowired
    private PasswordEncoder encoder;

    // La página es de React; lo que debe quedar abierto sin sesión es el desafío que usa su formulario
    @Test
    void laPaginaAbre() throws Exception {
        mvc.perform(get("/api/auth/captcha")).andExpect(status().isOk());
    }

    @Test
    void noSeSaltaAlPasoDeVerificar() throws Exception {
        crearUsuario("ana@sena.edu.co", "123456");
        mvc.perform(post("/api/auth/recuperacion/verificar").contentType(MediaType.APPLICATION_JSON)
                .content(aJson(Map.of("correo", "ana@sena.edu.co", "codigo", "123456")))).andExpect(status().isBadRequest());
    }

    @Test
    void noSeSaltaAlPasoDeCambiar() throws Exception {
        crearUsuario("ana@sena.edu.co", "123456");
        mvc.perform(post("/api/auth/recuperacion/cambiar").contentType(MediaType.APPLICATION_JSON)
                        .content(aJson(Map.of("correo", "ana@sena.edu.co", "permiso", "inventado",
                                "password", "NuevaClave2026", "confirmacion", "NuevaClave2026"))))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void cualquierPerfilPideCodigo(Perfil perfil) throws Exception {
        Usuario usuario = crearUsuario(perfil.correo(), perfil.cargo, perfil.rol, "3000000009", u -> { });
        pedirCodigo(usuario.getCorreo());
        assertThat(porCorreo(usuario.getCorreo()).getCodigoRecuperacion()).isNotNull();
    }

    @Test
    void cambiaLaContrasenaConElCodigo() throws Exception {
        crearUsuario("ana@sena.edu.co", "123456");
        pedirCodigo("ana@sena.edu.co");
        String codigo = porCorreo("ana@sena.edu.co").getCodigoRecuperacion();
        String permiso = leer(mvc.perform(post("/api/auth/recuperacion/verificar").contentType(MediaType.APPLICATION_JSON)
                .content(aJson(Map.of("correo", "ana@sena.edu.co", "codigo", codigo)))).andReturn()).get("permiso").asText();
        mvc.perform(post("/api/auth/recuperacion/cambiar").contentType(MediaType.APPLICATION_JSON)
                .content(aJson(Map.of("correo", "ana@sena.edu.co", "permiso", permiso, "password", "NuevaClave2026",
                        "confirmacion", "NuevaClave2026")))).andExpect(status().isOk());
        assertThat(encoder.matches("NuevaClave2026", porCorreo("ana@sena.edu.co").getContrasena())).isTrue();
    }
}
