package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.service.CorreoService;
import co.sena.adso.porteria.soporte.Excel;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

// Portería 2: tests/modulos/test_importacion_excel.py
class ImportacionExcelTest extends PruebaIntegracion {

    private static final Pattern CLAVE_TEMPORAL = Pattern.compile("Contraseña temporal:</strong>\\s*(.+?)</p>");

    // Intercepta el correo de bienvenida: es el único sitio donde aparece la contraseña temporal
    @MockBean
    private CorreoService correoService;

    @Autowired
    private PasswordEncoder encoder;

    private Sesion admin() throws Exception {
        Usuario admin = crearUsuario("admin@sena.edu.co", "Administrador", "Admin", "1000000001", u -> { });
        return new Sesion(admin, iniciarSesion(admin.getCorreo(), CLAVE));
    }

    private static Map<String, Object> fila(Object... claveValor) {
        Map<String, Object> fila = new HashMap<>();
        for (int i = 0; i < claveValor.length; i += 2) {
            fila.put((String) claveValor[i], claveValor[i + 1]);
        }
        return fila;
    }

    @SafeVarargs
    private MvcResult importar(Sesion sesion, String nombre, Map<String, Object>... filas) throws Exception {
        return mvc.perform(con(sesion, multipart("/api/admin/usuarios/importar").file(Excel.deFilas(List.of(filas), nombre))))
                .andReturn();
    }

    @SafeVarargs
    private MvcResult importar(Sesion sesion, Map<String, Object>... filas) throws Exception {
        return importar(sesion, "usuarios.xlsx", filas);
    }

    private JsonNode detalles(MvcResult r) throws Exception {
        return leer(r).get("detalles");
    }

    private List<String> errores(MvcResult r) throws Exception {
        List<String> lista = new ArrayList<>();
        detalles(r).get("errores").forEach(e -> lista.add(e.asText().toLowerCase()));
        return lista;
    }

    private String claveDelCorreo(String destinatario) {
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviar(eq(destinatario), anyString(), cuerpo.capture());
        Matcher m = CLAVE_TEMPORAL.matcher(cuerpo.getValue());
        return m.find() ? m.group(1).trim() : null;
    }

    @Nested
    class DocumentoNoSeCorrompe {

        // Con la columna medio vacía, Excel guarda el número como decimal y llegaba "1098765432.0"
        @Test
        void unaCeldaVaciaNoConvierteLosDocumentosEnDecimales() throws Exception {
            MvcResult r = importar(admin(), fila("Nombre", "Ana Ruiz", "Correo", "ana@sena.edu.co", "Documento", 1098765432L),
                    fila("Nombre", "Beto Gil", "Correo", "beto@sena.edu.co", "Documento", null));
            assertThat(r.getResponse().getStatus()).isEqualTo(200);
            assertThat(porCorreo("ana@sena.edu.co").getDocumento()).isEqualTo("1098765432").doesNotContain(".");
            assertThat(porCorreo("beto@sena.edu.co").getDocumento()).isNull();
        }

        // Misma causa que el documento: la ficha se imprime en el carnet
        @Test
        void laFichaTampocoSeCorrompe() throws Exception {
            importar(admin(), fila("Nombre", "Ana Ruiz", "Correo", "ana@sena.edu.co", "Cargo", "Aprendiz", "Ficha", 2894567),
                    fila("Nombre", "Beto Gil", "Correo", "beto@sena.edu.co", "Cargo", "Aprendiz", "Ficha", null));
            assertThat(porCorreo("ana@sena.edu.co").getFicha()).isEqualTo("2894567");
        }

        // Guardar un documento con letras es peor que no guardarlo: nunca coincide con el de portería
        @Test
        void unDocumentoInvalidoNoSeGuarda() throws Exception {
            MvcResult r = importar(admin(), fila("Nombre", "Ana Ruiz", "Correo", "ana@sena.edu.co", "Documento", "10A98"));
            assertThat(porCorreo("ana@sena.edu.co")).isNotNull();
            assertThat(porCorreo("ana@sena.edu.co").getDocumento()).isNull();
            assertThat(errores(r)).anyMatch(a -> a.contains("documento"));
        }

        @Test
        void elDocumentoSeNormalizaComoEnPorteria() throws Exception {
            importar(admin(), fila("Nombre", "Ana Ruiz", "Correo", "ana@sena.edu.co", "Documento", "1.098.765.432"));
            assertThat(porCorreo("ana@sena.edu.co").getDocumento()).isEqualTo("1098765432");
        }
    }

    @Nested
    class ListaBlancaDeRoles {

        // Quien prepara el Excel no puede repartir acceso total sin que el admin que lo sube se entere
        @Test
        void unaFilaQuePideAdminSeDegradaAUsuario() throws Exception {
            MvcResult r = importar(admin(), fila("Nombre", "Intruso", "Correo", "intruso@sena.edu.co", "Rol", "Admin"));
            Usuario creado = porCorreo("intruso@sena.edu.co");
            assertThat(creado.getRol().getNombre()).isEqualTo("Usuario");
            assertThat(creado.esAdmin()).isFalse();
            assertThat(errores(r)).anyMatch(a -> a.contains("rol"));
        }

        @Test
        void unCargoInventadoSeDegradaAAprendiz() throws Exception {
            importar(admin(), fila("Nombre", "Intruso", "Correo", "intruso@sena.edu.co", "Cargo", "Celador Jefe Supremo"));
            Usuario creado = porCorreo("intruso@sena.edu.co");
            assertThat(creado.getCargo()).isEqualTo("Aprendiz");
            assertThat(creado.puedeOperarPorteria()).isFalse();
        }

        @Test
        void laImportacionNoLaPuedeLanzarCualquiera() throws Exception {
            MvcResult r = importar(entrarComo(Perfil.APRENDIZ), fila("Nombre", "Ana Ruiz", "Correo", "ana@sena.edu.co"));
            assertThat(r.getResponse().getStatus()).isEqualTo(403);
            assertThat(porCorreo("ana@sena.edu.co")).isNull();
        }
    }

    @Nested
    class FilasProblematicas {

        @Test
        void unCorreoRepetidoSeOmiteYElRestoEntra() throws Exception {
            crearUsuario("ana@sena.edu.co", "1098765432");
            MvcResult r = importar(admin(), fila("Nombre", "Ana Repetida", "Correo", "ana@sena.edu.co"),
                    fila("Nombre", "Beto Gil", "Correo", "beto@sena.edu.co"));
            assertThat(detalles(r).get("omitidos").asInt()).isEqualTo(1);
            assertThat(detalles(r).get("creados").asInt()).isEqualTo(1);
            assertThat(porCorreo("beto@sena.edu.co")).isNotNull();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE correo = 'ana@sena.edu.co'", Long.class)).isEqualTo(1);
        }

        // La columna es única: sin comprobarlo, el choque tumbaría también las filas buenas
        @Test
        void unDocumentoRepetidoNoTumbaElLote() throws Exception {
            crearUsuario("previa@sena.edu.co", "1098765432");
            MvcResult r = importar(admin(), fila("Nombre", "Ana Ruiz", "Correo", "ana@sena.edu.co", "Documento", 1098765432L),
                    fila("Nombre", "Beto Gil", "Correo", "beto@sena.edu.co", "Documento", 1012345678L));
            assertThat(r.getResponse().getStatus()).isEqualTo(200);
            assertThat(porCorreo("ana@sena.edu.co").getDocumento()).isNull();
            assertThat(porCorreo("beto@sena.edu.co").getDocumento()).isEqualTo("1012345678");
        }

        // Falla el correo de bienvenida de una fila: el lote sigue y lo anota como aviso
        @Test
        void unaFilaCorruptaNoImpideQueSeImportenLasDemas() throws Exception {
            doThrow(new RuntimeException("servidor SMTP caído")).when(correoService)
                    .enviar(eq("beto@sena.edu.co"), anyString(), anyString());
            MvcResult r = importar(admin(), fila("Nombre", "Ana Ruiz", "Correo", "ana@sena.edu.co"),
                    fila("Nombre", "Beto Gil", "Correo", "beto@sena.edu.co"),
                    fila("Nombre", "Caro Diaz", "Correo", "caro@sena.edu.co"));
            assertThat(r.getResponse().getStatus()).isEqualTo(200);
            for (String correo : List.of("ana@sena.edu.co", "beto@sena.edu.co", "caro@sena.edu.co")) {
                assertThat(porCorreo(correo)).isNotNull();
            }
            assertThat(detalles(r).get("creados").asInt()).isEqualTo(3);
        }

        @Test
        void lasFilasVaciasDelFinalSeOmitenSinAvisos() throws Exception {
            MvcResult r = importar(admin(), fila("Nombre", "Ana Ruiz", "Correo", "ana@sena.edu.co"),
                    fila("Nombre", null, "Correo", null, "Documento", 1098765432L));
            assertThat(detalles(r).get("creados").asInt()).isEqualTo(1);
            assertThat(detalles(r).get("omitidos").asInt()).isEqualTo(1);
            assertThat(errores(r)).isEmpty();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE nombre = 'nan'", Long.class)).isZero();
        }
    }

    @Nested
    class ContrasenasTemporales {

        // Una contraseña fija en el código abre cualquier cuenta importada antes de que su dueño la use
        @Test
        void cadaFilaRecibeUnaContrasenaDistinta() throws Exception {
            importar(admin(), fila("Nombre", "Ana Ruiz", "Correo", "ana@sena.edu.co"),
                    fila("Nombre", "Beto Gil", "Correo", "beto@sena.edu.co"));
            Usuario ana = porCorreo("ana@sena.edu.co");
            Usuario beto = porCorreo("beto@sena.edu.co");
            String claveAna = claveDelCorreo("ana@sena.edu.co");
            String claveBeto = claveDelCorreo("beto@sena.edu.co");

            assertThat(claveAna).isNotBlank().isNotEqualTo(claveBeto);
            assertThat(claveBeto).isNotBlank();
            assertThat(encoder.matches(claveAna, ana.getContrasena())).isTrue();
            assertThat(encoder.matches(claveBeto, beto.getContrasena())).isTrue();
            assertThat(encoder.matches(claveAna, beto.getContrasena())).isFalse();
            for (String previsible : List.of("sena123", "Sena123*", "123456", "password", "cambiar123", "Temporal123")) {
                assertThat(encoder.matches(previsible, ana.getContrasena())).isFalse();
            }
            assertThat(ana.isDebeCambiarContrasena()).isTrue();
        }

        @Test
        void losHashesNoSeRepiten() throws Exception {
            importar(admin(), fila("Nombre", "Ana Ruiz", "Correo", "ana@sena.edu.co"),
                    fila("Nombre", "Beto Gil", "Correo", "beto@sena.edu.co"));
            assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT contrasena) FROM usuarios "
                    + "WHERE correo IN ('ana@sena.edu.co', 'beto@sena.edu.co')", Long.class)).isEqualTo(2);
        }
    }

    // La masiva no dejaba rastro, justo donde se reparten roles y cargos a mucha gente de una vez
    @Nested
    class Auditoria {

        @Test
        void elLoteQuedaRegistrado() throws Exception {
            Sesion admin = admin();
            importar(admin, "aprendices_2026.xlsx", fila("Nombre", "Ana Ruiz", "Correo", "ana@sena.edu.co"),
                    fila("Nombre", "Beto Gil", "Correo", "beto@sena.edu.co"));
            Map<String, Object> registro = jdbc.queryForMap(
                    "SELECT usuario_id, detalles FROM auditoria WHERE accion = 'Importación masiva de usuarios'");
            assertThat(registro.get("usuario_id")).isEqualTo(admin.usuario().getId());
            assertThat((String) registro.get("detalles")).contains("aprendices_2026.xlsx", "Creados: 2");
        }
    }
}
