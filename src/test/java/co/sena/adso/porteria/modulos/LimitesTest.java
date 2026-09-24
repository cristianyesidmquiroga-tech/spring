package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.config.LimitePeticionesFilter;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.ArrayList;
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
}
