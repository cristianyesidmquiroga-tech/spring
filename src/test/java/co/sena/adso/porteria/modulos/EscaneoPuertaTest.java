package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Rol;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

// Portería 2: tests/modulos/test_escaneo_puerta.py
class EscaneoPuertaTest extends PruebaIntegracion {

    private static final String INCIDENTE = "Incidente registrado en portería";

    private Usuario aprendiz(String correo, String documento, Consumer<Usuario> ajustes) {
        return crearUsuario(correo, "Aprendiz", Rol.USUARIO, documento, ajustes);
    }

    private MvcResult verificar(Sesion sesion, String codigo) throws Exception {
        return mvc.perform(con(sesion, get("/api/porteria/verificar").param("codigo", codigo))).andReturn();
    }

    private long incidentes() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM auditoria WHERE accion = ?", Long.class, INCIDENTE);
    }

    @Test
    void documentoValidoDevuelveDatosDelUsuario() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Usuario juan = aprendiz("aprendiz@sena.edu.co", "123123", u -> u.setNombre("Juan Perez"));

        MvcResult r = verificar(celador, juan.getDocumento());
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        JsonNode data = leer(r);
        assertThat(data.get("encontrado").asBoolean()).isTrue();
        assertThat(data.get("id").asLong()).isEqualTo(juan.getId());
        assertThat(data.get("nombre").asText()).isEqualTo("Juan Perez");
        assertThat(data.get("documento").asText()).isEqualTo("123123");
        assertThat(data.get("tipo").asText()).isEqualTo("Usuario");
        assertThat(data.get("estado").asText()).isEqualTo("Afuera");
    }

    @Test
    void documentoInexistenteNoSeEncuentra() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);

        MvcResult r = verificar(celador, "999999999");
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(leer(r)).isEqualTo(json.readTree("{\"encontrado\": false}"));
    }

    @Test
    void documentoDePerfilIncompletoSePuedeVerificar() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Usuario incompleto = aprendiz("incompleto@sena.edu.co", "555555", u -> u.setPerfilCompleto(false));

        MvcResult r = verificar(celador, incompleto.getDocumento());
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(leer(r).get("encontrado").asBoolean()).isTrue();
        assertThat(leer(r).get("id").asLong()).isEqualTo(incompleto.getId());
    }

    @Test
    void fotoNoAprobadaSeMarcaExplicitamente() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Usuario sinFoto = aprendiz("sinfoto@sena.edu.co", "444444", u -> { });

        mvc.perform(con(celador, get("/api/porteria/verificar").param("codigo", sinFoto.getDocumento())))
                .andExpect(jsonPath("$.fotoAprobada").value(false));
    }

    @Test
    void fotoAprobadaSeMarcaTrue() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Usuario aprobado = aprendiz("aprobado@sena.edu.co", "333333",
                u -> u.revisarFoto(true, null, null, LocalDateTime.now()));

        mvc.perform(con(celador, get("/api/porteria/verificar").param("codigo", aprobado.getDocumento())))
                .andExpect(jsonPath("$.fotoAprobada").value(true));
    }

    // La API tiene un solo endpoint de verificación: la "versión página" de Flask se revisa sobre el mismo JSON
    @Test
    void documentoValidoMuestraPerfil() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Usuario juan = aprendiz("aprendiz@sena.edu.co", "123123", u -> u.setNombre("Juan Perez"));

        mvc.perform(con(celador, get("/api/porteria/verificar").param("codigo", juan.getDocumento())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Juan Perez"));
    }

    @Test
    void documentoInexistenteRedirigeConAviso() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);

        mvc.perform(con(celador, get("/api/porteria/verificar").param("codigo", "999999999")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encontrado").value(false))
                .andExpect(jsonPath("$.nombre").doesNotExist());
    }

    @Test
    void perfilIncompletoSePuedeVer() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Usuario incompleto = aprendiz("incompleto@sena.edu.co", "555555", u -> {
            u.setPerfilCompleto(false);
            u.setNombre("Perfil Incompleto");
        });

        mvc.perform(con(celador, get("/api/porteria/verificar").param("codigo", incompleto.getDocumento())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Perfil Incompleto"));
    }

    @Test
    void fotoNoAprobadaNoImpideVerLaPagina() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Usuario sinFoto = aprendiz("sinfoto@sena.edu.co", "444444", u -> u.setNombre("Sin Foto Aun"));

        mvc.perform(con(celador, get("/api/porteria/verificar").param("codigo", sinFoto.getDocumento())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Sin Foto Aun"));
    }

    @Test
    void aprendizNoConsultaApiVerify() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        Usuario otro = aprendiz("otro@sena.edu.co", "456456", u -> { });

        MvcResult r = verificar(aprendiz, otro.getDocumento());
        assertThat(r.getResponse().getStatus()).isEqualTo(403);
        assertThat(r.getResponse().getContentAsString()).doesNotContain("456456");
    }

    @Test
    void aprendizNoConsultaVerifyPagina() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        Usuario otro = aprendiz("otro@sena.edu.co", "456456", u -> u.setNombre("Datos Ajenos"));

        MvcResult r = verificar(aprendiz, otro.getDocumento());
        assertThat(r.getResponse().getStatus()).isEqualTo(403);
        assertThat(r.getResponse().getContentAsString()).doesNotContain("Datos Ajenos");
    }

    @Test
    void noAutenticadoNoConsulta() throws Exception {
        Usuario aprendiz = aprendiz("aprendiz@sena.edu.co", "123123", u -> { });

        mvc.perform(get("/api/porteria/verificar").param("codigo", aprendiz.getDocumento()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void celadorRegistraIncidente() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Usuario aprendiz = aprendiz("aprendiz@sena.edu.co", "123123", u -> { });

        mvc.perform(conJson(celador, post("/api/porteria/incidentes"), Map.of(
                        "entidadId", aprendiz.getId(),
                        "tipoEntidad", "Usuario",
                        "detalles", "Documento fisico no coincide con la foto del carnet.")))
                .andExpect(status().isCreated());

        List<Map<String, Object>> filas = jdbc.queryForList(
                "SELECT usuario_id, registro_id, detalles FROM auditoria WHERE accion = ?", INCIDENTE);
        assertThat(filas).hasSize(1);
        assertThat(((Number) filas.get(0).get("usuario_id")).longValue()).isEqualTo(celador.usuario().getId());
        assertThat(((Number) filas.get(0).get("registro_id")).longValue()).isEqualTo(aprendiz.getId());
        assertThat((String) filas.get(0).get("detalles")).contains("no coincide");
    }

    @Test
    void incidenteSinDetallesNoSeRegistra() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);

        mvc.perform(conJson(celador, post("/api/porteria/incidentes"), Map.of("entidadId", 1, "tipoEntidad", "Usuario")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
        assertThat(incidentes()).isZero();
    }

    @Test
    void aprendizNoRegistraIncidente() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);

        mvc.perform(conJson(aprendiz, post("/api/porteria/incidentes"), Map.of(
                        "entidadId", 1, "tipoEntidad", "Usuario", "detalles", "Intento de un aprendiz.")))
                .andExpect(status().isForbidden());
        assertThat(incidentes()).isZero();
    }

    // El JSON tipa entidadId como número: un código no numérico como SENA-VISIT:abc llega al API sin id
    @Test
    void entidadIdNoNumericoNoRevienta() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        mvc.perform(conJson(celador, post("/api/porteria/incidentes"), Map.of("entidadId", "SENA-VISIT:abc",
                        "tipoEntidad", "Visitante", "detalles", "Un visitante sin identificacion clara.")))
                .andExpect(status().isBadRequest());

        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("entidadId", null);
        cuerpo.put("tipoEntidad", "Visitante");
        cuerpo.put("detalles", "Un visitante sin identificacion clara.");

        mvc.perform(conJson(celador, post("/api/porteria/incidentes"), cuerpo)).andExpect(status().isCreated());

        List<Long> registros = jdbc.queryForList("SELECT registro_id FROM auditoria WHERE accion = ?", Long.class, INCIDENTE);
        assertThat(registros).containsExactly(0L);
    }
}
