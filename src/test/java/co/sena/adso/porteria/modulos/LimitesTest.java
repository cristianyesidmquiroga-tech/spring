package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.config.LimitePeticionesFilter;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.service.CaptchaService;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

// Portería 2: tests/modulos/test_limites.py
class LimitesTest extends PruebaIntegracion {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping rutas;

    private Set<String> endpointsReales() {
        Set<String> reales = new HashSet<>();
        rutas.getHandlerMethods().keySet().forEach(info -> info.getPatternValues().forEach(ruta ->
                info.getMethodsCondition().getMethods().forEach(m -> reales.add(m.name() + " " + ruta))));
        return reales;
    }

    private MvcResult loginFallido() throws Exception {
        return loginFallidoDesde("127.0.0.1");
    }

    // MockMvc no aplica X-Forwarded-For (en producción lo hace Tomcat): la IP se pone directo
    private MvcResult loginFallidoDesde(String ip) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(aJson(Map.of("identificador", "ana@sena.edu.co", "password", "incorrecta1")))
                .with(r -> {
                    r.setRemoteAddr(ip);
                    return r;
                })).andReturn();
    }

    @Test
    void todosLosEndpointsLimitadosExisten() {
        List<String> declarados = new ArrayList<>();
        for (Object regla : (List<?>) ReflectionTestUtils.getField(LimitePeticionesFilter.class, "LIMITES")) {
            declarados.add(ReflectionTestUtils.invokeMethod(regla, "metodo") + " "
                    + ReflectionTestUtils.invokeMethod(regla, "ruta"));
        }
        assertThat(declarados).isNotEmpty();
        assertThat(endpointsReales()).containsAll(declarados);
    }

    // En la API no hay lista de exentos: exento es todo lo que no está en el catálogo
    @Test
    void todosLosEndpointsExentosExisten() throws Exception {
        assertThat(endpointsReales()).contains("GET /api/porteria/verificar", "POST /api/porteria/movimientos",
                "POST /api/porteria/incidentes");
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void elLimiteSaltaPorIp() throws Exception {
        crearUsuario("ana@sena.edu.co", "123456");
        List<Integer> codigos = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            codigos.add(loginFallido().getResponse().getStatus());
        }
        assertThat(codigos).as("El límite de login nunca saltó").contains(429);
    }

    @Test
    void elMensajeJsonEsEntendible() throws Exception {
        crearUsuario("ana@sena.edu.co", "123456");
        MvcResult respuesta = null;
        for (int i = 0; i < 12; i++) {
            respuesta = loginFallido();
            if (respuesta.getResponse().getStatus() == 429) {
                break;
            }
        }
        assertThat(respuesta.getResponse().getStatus()).isEqualTo(429);
        String mensaje = leer(respuesta).get("mensaje").asText().toLowerCase();
        assertThat(mensaje).contains("demasiadas peticiones").contains("minuto");
        assertThat(respuesta.getResponse().getHeader("Retry-After")).isNotBlank();
    }

    @Test
    void elMensajeHtmlEsUnaPagina() throws Exception {
        crearUsuario("ana@sena.edu.co", "123456");
        MvcResult respuesta = null;
        for (int i = 0; i < 12; i++) {
            respuesta = loginFallido();
            if (respuesta.getResponse().getStatus() == 429) {
                break;
            }
        }
        assertThat(respuesta.getResponse().getStatus()).isEqualTo(429);
        String texto = respuesta.getResponse().getContentAsString().toLowerCase();
        assertThat(texto.contains("demasiado rápido") || texto.contains("demasiadas")).isTrue();
    }

    @Test
    void cadaIpTieneSuPropioContador() throws Exception {
        crearUsuario("ana@sena.edu.co", "123456");
        for (int i = 0; i < 12; i++) {
            loginFallidoDesde("10.0.0.1");
        }
        assertThat(loginFallidoDesde("10.0.0.2").getResponse().getStatus()).isNotEqualTo(429);
    }

    @Test
    void muchasVerificacionesSeguidas() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        crearUsuario("aprendiz@sena.edu.co", "123123");
        Set<Integer> codigos = new HashSet<>();
        for (int i = 0; i < 150; i++) {
            codigos.add(mvc.perform(con(celador, get("/api/porteria/verificar").param("codigo", "123123")))
                    .andReturn().getResponse().getStatus());
        }
        assertThat(codigos).doesNotContain(429);
    }

    @Test
    void muchosMovimientosSeguidos() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Usuario aprendiz = crearUsuario("aprendiz@sena.edu.co", "123123");
        Set<Integer> codigos = new HashSet<>();
        for (int i = 0; i < 150; i++) {
            codigos.add(mvc.perform(conJson(celador, post("/api/porteria/movimientos"),
                            Map.of("tipoEntidad", "Usuario", "entidadId", aprendiz.getId(), "tipo", "Entrada")))
                    .andReturn().getResponse().getStatus());
        }
        assertThat(codigos).doesNotContain(429);
    }

    // Una auditoría envió 50 mensajes seguidos sin ninguna traba
    @Test
    void elCentroDeAyudaSeLimita() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        Set<Integer> codigos = new HashSet<>();
        for (int i = 0; i < 8; i++) {
            codigos.add(mvc.perform(conJson(aprendiz, post("/api/ayuda/contacto"),
                    Map.of("asunto", "Otro", "detalle", "Hola, necesito ayuda."))).andReturn().getResponse().getStatus());
        }
        assertThat(codigos).contains(429);
    }

    @Autowired
    private CaptchaService captchaService;

    // Resuelve la prueba de trabajo como lo haría el navegador
    private String resolver(JsonNode desafio) throws Exception {
        String salt = desafio.get("salt").asText();
        for (int numero = 0; numero <= desafio.get("maxNumber").asInt(); numero++) {
            if (CaptchaService.sha256(salt + numero).equals(desafio.get("challenge").asText())) {
                return Base64.getEncoder().encodeToString(aJson(Map.of("algorithm", "SHA-256",
                        "challenge", desafio.get("challenge").asText(), "number", numero, "salt", salt,
                        "signature", desafio.get("signature").asText())).getBytes(StandardCharsets.UTF_8));
            }
        }
        throw new AssertionError("El desafío no tiene solución");
    }

    private JsonNode desafio() throws Exception {
        return leer(mvc.perform(get("/api/auth/captcha")).andReturn());
    }

    // Espacio de búsqueda pequeño: la prueba resuelve la prueba de trabajo de verdad
    private void encenderDesafio() {
        ReflectionTestUtils.setField(captchaService, "activo", true);
        ReflectionTestUtils.setField(captchaService, "dificultad", 500);
    }

    // Si la verificación estorba se apaga desde el .env y nadie se queda sin crear cuenta
    @Test
    void apagadoElRegistroSigueFuncionando() throws Exception {
        assertThat(captchaService.activo()).isFalse();
        assertThat(registrar(datosRegistro()).getResponse().getStatus()).isEqualTo(201);
        assertThat(porCorreo("nueva@sena.edu.co")).isNotNull();
    }

    @Test
    void apagadoLaRecuperacionSigueFuncionando() throws Exception {
        crearUsuario("ana@sena.edu.co", "123456");
        assertThat(pedirCodigo("ana@sena.edu.co").getResponse().getStatus()).isEqualTo(200);
    }

    // React solo pinta el widget si la API dice que está activo
    @Test
    void apagadoElWidgetNoSePinta() throws Exception {
        assertThat(desafio().get("activo").asBoolean()).isFalse();
        assertThat(desafio().has("challenge")).isFalse();
    }

    @Test
    void encendidoSinSolucionNoRegistra() throws Exception {
        encenderDesafio();
        assertThat(registrar(datosRegistro()).getResponse().getStatus()).isEqualTo(400);
        assertThat(porCorreo("nueva@sena.edu.co")).isNull();
    }

    @Test
    void encendidoConSolucionSiRegistra() throws Exception {
        encenderDesafio();
        Map<String, Object> datos = datosRegistro();
        datos.put("captcha", resolver(desafio()));
        assertThat(registrar(datos).getResponse().getStatus()).isEqualTo(201);
    }

    // Sin esto un bot resuelve un desafío y reenvía el formulario mil veces
    @Test
    void unaSolucionNoSirveDosVeces() throws Exception {
        encenderDesafio();
        String solucion = resolver(desafio());
        Map<String, Object> primero = datosRegistro();
        primero.put("captcha", solucion);
        assertThat(registrar(primero).getResponse().getStatus()).isEqualTo(201);

        Map<String, Object> segundo = datosRegistro();
        segundo.put("correo", "otra@sena.edu.co");
        segundo.put("documento", "999777");
        segundo.put("captcha", solucion);
        assertThat(registrar(segundo).getResponse().getStatus()).isEqualTo(400);
        assertThat(porCorreo("otra@sena.edu.co")).isNull();
    }

    // La firma HMAC es lo que impide fabricarse un desafío propio
    @Test
    void unaSolucionInventadaNoPasa() throws Exception {
        encenderDesafio();
        Map<String, Object> datos = datosRegistro();
        datos.put("captcha", Base64.getEncoder().encodeToString(aJson(Map.of("algorithm", "SHA-256",
                "challenge", "a".repeat(64), "number", 1, "salt", "abc?expires=99999999999&",
                "signature", "b".repeat(64))).getBytes(StandardCharsets.UTF_8)));
        assertThat(registrar(datos).getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void encendidoElWidgetSePinta() throws Exception {
        encenderDesafio();
        JsonNode desafio = desafio();
        assertThat(desafio.get("activo").asBoolean()).isTrue();
        assertThat(desafio.get("challenge").asText()).hasSize(64);
    }
}
