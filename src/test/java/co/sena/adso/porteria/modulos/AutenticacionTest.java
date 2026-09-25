package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.service.CuentaService;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

// Portería 2: tests/modulos/test_autenticacion.py
class AutenticacionTest extends PruebaIntegracion {

    @Autowired
    private CuentaService cuentaService;

    @Autowired
    private PasswordEncoder encoder;

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

    private MvcResult verificarCodigo(String correo, String codigo) throws Exception {
        return mvc.perform(post("/api/auth/recuperacion/verificar").contentType(MediaType.APPLICATION_JSON)
                .content(aJson(Map.of("correo", correo, "codigo", codigo)))).andReturn();
    }

    @Test
    void respuestaIdenticaExistaONoLaCuenta() throws Exception {
        crearUsuario("ana@sena.edu.co", "123456");
        MvcResult con = pedirCodigo("ana@sena.edu.co");
        MvcResult sin = pedirCodigo("fantasma@sena.edu.co");
        assertThat(con.getResponse().getStatus()).isEqualTo(sin.getResponse().getStatus());
        assertThat(con.getResponse().getContentAsString()).isEqualTo(sin.getResponse().getContentAsString());
    }

    // Sin tope, un código de 6 dígitos se prueba hasta acertar; pero fallarlo no puede bloquear el login
    @Test
    void codigoIncorrectoSeAnulaTrasCincoIntentos() throws Exception {
        Usuario usuario = crearUsuario("ana@sena.edu.co", "123456");
        pedirCodigo("ana@sena.edu.co");
        for (int i = 0; i < 5; i++) {
            verificarCodigo("ana@sena.edu.co", "000000");
        }
        assertThat(recargar(usuario).getCodigoRecuperacion()).as("el código debió anularse").isNull();
        assertThat(recargar(usuario).getBloqueadoHasta()).as("la recuperación no debe bloquear el login").isNull();
    }

    // Si compartieran contador, pedir un código entre tandas permitiría probar contraseñas sin límite
    @Test
    void pedirCodigoNoReiniciaElBloqueoDelLogin() throws Exception {
        Usuario usuario = crearUsuario("ana@sena.edu.co", "123456");
        for (int i = 0; i < 4; i++) {
            login("ana@sena.edu.co", "incorrecta1");
        }
        assertThat(recargar(usuario).getIntentosFallidos()).isEqualTo(4);
        pedirCodigo("ana@sena.edu.co");
        assertThat(recargar(usuario).getIntentosFallidos()).as("el contador del login no debe reiniciarse").isEqualTo(4);
        login("ana@sena.edu.co", "incorrecta1");
        assertThat(recargar(usuario).getBloqueadoHasta()).as("el quinto intento debe bloquear").isNotNull();
    }

    @Test
    void noSePuedeSaltarAlPasoDeCambio() throws Exception {
        crearUsuario("ana@sena.edu.co", "123456");
        pedirCodigo("ana@sena.edu.co");
        MvcResult r = mvc.perform(post("/api/auth/recuperacion/cambiar").contentType(MediaType.APPLICATION_JSON)
                .content(aJson(Map.of("correo", "ana@sena.edu.co", "password", "NuevaClave2026",
                        "confirmacion", "NuevaClave2026")))).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(400);
        assertThat(encoder.matches("NuevaClave2026", porCorreo("ana@sena.edu.co").getContrasena())).isFalse();
    }

    @Test
    void codigoExpiradoSeRechaza() throws Exception {
        Usuario usuario = crearUsuario("ana@sena.edu.co", "123456");
        pedirCodigo("ana@sena.edu.co");
        String codigo = recargar(usuario).getCodigoRecuperacion();
        jdbc.update("UPDATE usuarios SET recuperacion_expiracion = now() - interval '1 day' WHERE id = ?", usuario.getId());

        assertThat(verificarCodigo("ana@sena.edu.co", codigo).getResponse().getStatus()).isEqualTo(400);
        assertThat(recargar(usuario).getCodigoRecuperacion()).isNull();
    }

    @Test
    void registroValidoCreaLaCuenta() throws Exception {
        assertThat(registrar(datosRegistro()).getResponse().getStatus()).isEqualTo(201);
        assertThat(porCorreo("nueva@sena.edu.co")).isNotNull();
    }

    // Ley 1581 de 2012: sin autorización expresa no se pueden tratar los datos personales
    @Test
    void sinAutorizacionDeDatosNoSeRegistra() throws Exception {
        Map<String, Object> datos = datosRegistro();
        datos.put("aceptaDatos", false);
        assertThat(registrar(datos).getResponse().getStatus()).isEqualTo(400);
        assertThat(porCorreo("nueva@sena.edu.co")).isNull();
    }

    @Test
    void contrasenaVaciaRechazada() throws Exception {
        Map<String, Object> datos = datosRegistro();
        datos.put("password", "");
        datos.put("confirmacion", "");
        assertThat(registrar(datos).getResponse().getStatus()).isEqualTo(400);
        assertThat(porCorreo("nueva@sena.edu.co")).isNull();
    }

    @Test
    void contrasenaCortaRechazada() throws Exception {
        Map<String, Object> datos = datosRegistro();
        datos.put("password", "abc1");
        datos.put("confirmacion", "abc1");
        assertThat(registrar(datos).getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void restriccionDeDominio() throws Exception {
        Object original = ReflectionTestUtils.getField(cuentaService, "dominios");
        ReflectionTestUtils.setField(cuentaService, "dominios", List.of("sena.edu.co"));
        try {
            Map<String, Object> datos = datosRegistro();
            datos.put("correo", "alguien@gmail.com");
            assertThat(registrar(datos).getResponse().getStatus()).isEqualTo(400);
            assertThat(porCorreo("alguien@gmail.com")).isNull();
        } finally {
            ReflectionTestUtils.setField(cuentaService, "dominios", original);
        }
    }
}
