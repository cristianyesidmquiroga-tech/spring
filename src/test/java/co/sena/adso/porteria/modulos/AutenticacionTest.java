package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

// Portería 2: tests/modulos/test_autenticacion.py
class AutenticacionTest extends PruebaIntegracion {

    private MvcResult login(String identificador, String clave) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(aJson(Map.of("identificador", identificador, "password", clave)))).andReturn();
    }

    private Usuario recargar(Usuario u) {
        return usuarioRepository.findById(u.getId()).orElseThrow();
    }

    @Test
    void credencialesCorrectas() throws Exception {
        crearUsuario("ana@sena.edu.co", "123456");
        MvcResult r = login("ana@sena.edu.co", CLAVE);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(leer(r).get("token").asText()).isNotBlank();
    }

    @Test
    void noRevelaSiLaCuentaExiste() throws Exception {
        crearUsuario("ana@sena.edu.co", "123456");
        MvcResult existente = login("ana@sena.edu.co", "ContrasenaMala1");
        MvcResult inexistente = login("nadie@sena.edu.co", "ContrasenaMala1");
        assertThat(leer(existente).get("mensaje").asText()).isEqualTo(leer(inexistente).get("mensaje").asText());
        assertThat(existente.getResponse().getStatus()).isEqualTo(inexistente.getResponse().getStatus());
    }

    @Test
    void bloqueaTrasCincoIntentos() throws Exception {
        Usuario usuario = crearUsuario("ana@sena.edu.co", "123456");
        for (int i = 0; i < 5; i++) {
            login("ana@sena.edu.co", "incorrecta1");
        }
        assertThat(recargar(usuario).getBloqueadoHasta()).isNotNull();

        assertThat(login("ana@sena.edu.co", CLAVE).getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void elContadorSeReiniciaAlAcertar() throws Exception {
        Usuario usuario = crearUsuario("ana@sena.edu.co", "123456");
        for (int i = 0; i < 3; i++) {
            login("ana@sena.edu.co", "incorrecta1");
        }
        login("ana@sena.edu.co", CLAVE);
        assertThat(recargar(usuario).getIntentosFallidos()).isZero();
    }

    // En la API un campo obligatorio vacío es 400 de validación; lo que importa es que no entrega token
    @Test
    void loginSinContrasenaNoEntra() throws Exception {
        crearUsuario("ana@sena.edu.co", "123456");
        MvcResult r = login("ana@sena.edu.co", "");
        assertThat(r.getResponse().getStatus()).isEqualTo(400);
        assertThat(leer(r).has("token")).isFalse();
    }
}
