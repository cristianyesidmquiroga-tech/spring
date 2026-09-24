package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.config.LimitePeticionesFilter;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

// Portería 2: tests/modulos/test_minimizacion.py
class MinimizacionTest extends PruebaIntegracion {

    private static final String EXPORTACION = "Exportación de histórico de accesos";
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Autowired
    private LimitePeticionesFilter limitador;

    @Value("${app.fotos.carpeta}")
    private String carpetaFotos;

    private void entradaAhora(Usuario persona) {
        jdbc.update("INSERT INTO accesos (punto_id, referencia_id, tipo_referencia, tipo, fecha) VALUES (1, ?, 'Usuario', 'Entrada', ?)",
                persona.getId(), Timestamp.valueOf(LocalDateTime.now(BOGOTA)));
    }

    private int exportaciones() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM auditoria WHERE accion = ?", Integer.class, EXPORTACION);
    }

    private Sesion preparar() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        Usuario persona = crearUsuario("ana@sena.edu.co", "Aprendiz", "Usuario", "111", u -> {
            u.setPrograma("ADSO");
            u.setFicha("2999999");
        });
        entradaAhora(persona);
        return admin;
    }

    private boolean limitado(String metodo, String ruta) {
        return !Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(limitador, "shouldNotFilter",
                new MockHttpServletRequest(metodo, ruta)));
    }

    @Test
    void porteriaSiVeElDocumentoCompleto() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        entradaAhora(crearUsuario("ana@sena.edu.co", "1098765432"));
        String cuerpo = mvc.perform(con(celador, get("/api/porteria/panel/accesos")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(cuerpo).contains("1098765432");
    }

    @Test
    void noMuestraElCorreoEnLasTarjetas() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        Usuario persona = crearUsuario("correo.secreto@sena.edu.co", "Aprendiz", "Usuario", "111",
                u -> u.registrarFotoNueva("user_x.jpg", LocalDateTime.now(BOGOTA)));
        String cuerpo = mvc.perform(con(admin, get("/api/admin/fotos")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain("correo.secreto@sena.edu.co").contains(persona.getNombre());
    }

    @Test
    void laListaEstaPaginada() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        for (int i = 0; i < 30; i++) {
            conFoto(crearUsuario("foto" + i + "@sena.edu.co", String.format("50000%03d", i)), Usuario.FOTO_APROBADA);
        }

        mvc.perform(con(admin, get("/api/admin/fotos").param("estado", "todos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(24))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(30))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.estado").value("todos"))
                .andExpect(jsonPath("$.pendientes").value(0));
        mvc.perform(con(admin, get("/api/admin/fotos").param("estado", "todos").param("pagina", "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(6))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void sinFechasNoExportaYAvisa() throws Exception {
        Sesion admin = preparar();
        MvcResult r = mvc.perform(con(admin, get("/api/porteria/panel/exportar")))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(r.getResponse().getContentType()).doesNotContain("text/csv");
        assertThat(leer(r).get("mensaje").asText()).isNotBlank();
        assertThat(exportaciones()).isZero();
    }

    @Test
    void conFechasExportaYDejaConstanciaEnAuditoria() throws Exception {
        Sesion admin = preparar();
        LocalDate hoy = LocalDate.now(BOGOTA);
        MvcResult r = mvc.perform(con(admin, get("/api/porteria/panel/exportar")
                        .param("desde", hoy.toString()).param("hasta", hoy.plusDays(1).toString())))
                .andExpect(status().isOk()).andReturn();
        assertThat(r.getResponse().getContentType()).contains("text/csv");
        assertThat(r.getResponse().getContentAsString()).contains("111");

        List<Map<String, Object>> auditoria = jdbc.queryForList(
                "SELECT usuario_id, detalles FROM auditoria WHERE accion = ?", EXPORTACION);
        assertThat(auditoria).hasSize(1);
        assertThat(((Number) auditoria.get(0).get("usuario_id")).longValue()).isEqualTo(admin.usuario().getId());
        assertThat((String) auditoria.get(0).get("detalles")).contains(hoy.toString());
    }

    @Test
    void rangoInvertidoSeRechaza() throws Exception {
        Sesion admin = preparar();
        LocalDate hoy = LocalDate.now(BOGOTA);
        mvc.perform(con(admin, get("/api/porteria/panel/exportar")
                        .param("desde", hoy.toString()).param("hasta", hoy.minusDays(1).toString())))
                .andExpect(status().isBadRequest());
        assertThat(exportaciones()).isZero();
    }

    @Test
    void elHistorialHtmlEstaEnElCatalogoDeLimites() {
        assertThat(limitado("GET", "/api/historial")).isTrue();
    }

    @Test
    void elEscanerDePorteriaSigueLibre() {
        assertThat(limitado("GET", "/api/porteria/verificar")).isFalse();
        assertThat(limitado("POST", "/api/porteria/movimientos")).isFalse();
    }

    @Test
    void aprobarDosVecesNoDuplicaMensajes() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        Usuario persona = crearUsuario("ana@sena.edu.co", "Aprendiz", "Usuario", "111", u -> u.setPerfilCompleto(false));
        conFoto(persona, Usuario.FOTO_PENDIENTE);

        aprobar(admin, persona).andExpect(status().isOk());
        aprobar(admin, persona).andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Esa foto ya estaba aprobada."));

        // El mensaje interno automático llega con la fase 4 (mensajes)
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria WHERE accion = 'Foto aprobada'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void laFotoSigueAprobadaTrasElSegundoIntento() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        Usuario persona = crearUsuario("ana@sena.edu.co", "Aprendiz", "Usuario", "111", u -> u.setPerfilCompleto(false));
        Path ruta = conFoto(persona, Usuario.FOTO_APROBADA);

        aprobar(admin, persona).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT foto_estado FROM usuarios WHERE id = ?", String.class, persona.getId()))
                .isEqualTo(Usuario.FOTO_APROBADA);
        assertThat(ruta).isRegularFile();
    }

    private ResultActions aprobar(Sesion admin, Usuario persona) throws Exception {
        return mvc.perform(conJson(admin, post("/api/admin/fotos/{id}/revision", persona.getId()),
                Map.of("aprobada", true)));
    }

    private Path conFoto(Usuario persona, String estado) throws Exception {
        String nombre = "user_" + persona.getId() + ".jpg";
        Path ruta = Files.createDirectories(Path.of(carpetaFotos)).resolve(nombre);
        Files.write(ruta, "contenido de prueba".getBytes(StandardCharsets.UTF_8));
        jdbc.update("UPDATE usuarios SET foto = ?, foto_estado = ? WHERE id = ?", nombre, estado, persona.getId());
        return ruta;
    }
}
