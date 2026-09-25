package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.dto.CatalogosResponseDTO.TipoDocumento;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.exception.DatoInvalidoException;
import co.sena.adso.porteria.service.DocumentoService;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// Portería 2: tests/modulos/test_documentos.py
class DocumentosTest extends PruebaIntegracion {

    private final DocumentoService documentos = new DocumentoService();

    private String error(String tipo, String numero) {
        try {
            documentos.validar(tipo, numero);
            return null;
        } catch (DatoInvalidoException e) {
            return e.getMessage();
        }
    }

    private Sesion entrar(Usuario usuario) throws Exception {
        return new Sesion(usuario, iniciarSesion(usuario.getCorreo(), CLAVE));
    }

    private Usuario sinDocumento(String correo) {
        return crearUsuario(correo, "Aprendiz", "Usuario", null, u -> { });
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', ignoreLeadingAndTrailingWhitespace = false, value = {
            "1.098.765.432|1098765432",
            "1 098 765 432|1098765432",
            "1098-765-432|1098765432",
            "  1098765432  |1098765432"})
    void quitaPuntosEspaciosYGuiones(String entrada, String esperado) {
        assertThat(documentos.normalizar(entrada)).isEqualTo(esperado);
    }

    @Test
    void elNumeroSeGuardaNormalizado() {
        assertThat(documentos.validar("CC", "1.098.765.432")).isEqualTo("1098765432");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"CC, 123456", "CC, 1098765432", "TI, 1012345678", "TI, 10123456789", "CE, 123456", "CE, 1234567",
            "PPT, 123456789", "PPT, 1234567890", "PA, AB123456"})
    void aceptaLongitudesValidas(String tipo, String numero) {
        assertThat(error(tipo, numero)).as("%s %s debió aceptarse", tipo, numero).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"CC, 12345", "CC, 12345678901", "TI, 123456", "CE, 12345678", "PPT, 12345"})
    void rechazaLongitudesInvalidas(String tipo, String numero) {
        assertThat(error(tipo, numero)).isNotNull().contains(String.valueOf(numero.length()));
    }

    @Test
    void elMensajeSugiereRevisarElTipo() {
        assertThat(error("TI", "123456").toLowerCase()).contains("tipo de documento");
    }

    @Test
    void rechazaLetrasDondeSoloVanNumeros() {
        assertThat(error("CC", "10987A5432")).isNotNull().contains("solo números");
    }

    @Test
    void elPasaporteSiAdmiteLetras() {
        assertThat(error("PA", "AV1234567")).isNull();
    }

    @Test
    void rechazaQueEmpiecePorCero() {
        assertThat(error("CC", "0123456789")).isNotNull().contains("cero");
    }

    @Test
    void rechazaVacio() {
        assertThat(error("CC", "   ")).isNotNull();
    }

    @Test
    void rechazaUnTipoInventado() {
        assertThat(error("XX", "1098765432")).isNotNull();
    }

    @Test
    void todosLosTiposDescribenSuFormato() {
        assertThat(documentos.catalogo()).isNotEmpty()
                .allSatisfy((TipoDocumento t) -> assertThat(t.formato()).as("falta el formato de %s", t.codigo()).isNotBlank());
    }

    @Test
    void sugiereTarjetaDeIdentidadConOnceDigitos() {
        assertThat(documentos.tipoProbable("10123456789")).isEqualTo("TI");
    }

    @Test
    void sugierePasaporteSiTieneLetras() {
        assertThat(documentos.tipoProbable("AB123456")).isEqualTo("PA");
    }

    @Test
    void guardarUnDocumentoValido() throws Exception {
        Usuario persona = sinDocumento("ana@sena.edu.co");
        mvc.perform(conJson(entrar(persona), put("/api/perfil"),
                        Map.of("documento", "1.098.765.432", "tipoDocumento", "CC", "tipoSangre", "O+")))
                .andExpect(status().isOk());
        Usuario guardada = usuarioRepository.findById(persona.getId()).orElseThrow();
        assertThat(guardada.getDocumento()).isEqualTo("1098765432");
        assertThat(guardada.getTipoDocumento()).isEqualTo("CC");
    }

    @Test
    void unDocumentoInvalidoSeRechaza() throws Exception {
        Usuario persona = sinDocumento("ana@sena.edu.co");
        mvc.perform(conJson(entrar(persona), put("/api/perfil"), Map.of("documento", "123", "tipoDocumento", "CC")))
                .andExpect(status().isBadRequest());
        assertThat(usuarioRepository.findById(persona.getId()).orElseThrow().getDocumento()).isNull();
    }

    // Un documento repetido es regla de negocio: la API responde 422 (BusinessException), no 400
    @Test
    void noSePuedeUsarElDocumentoDeOtro() throws Exception {
        crearUsuario("beto@sena.edu.co", "1098765432");
        Usuario ana = sinDocumento("ana@sena.edu.co");
        mvc.perform(conJson(entrar(ana), put("/api/perfil"), Map.of("documento", "1098765432", "tipoDocumento", "CC")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje", Matchers.containsString("ya está registrado")));
    }

    private Map<String, Object> registroCon(String documento, String tipo) {
        Map<String, Object> datos = datosRegistro();
        datos.put("documento", documento);
        datos.put("tipoDocumento", tipo);
        return datos;
    }

    @Test
    void registroConDocumentoValido() throws Exception {
        assertThat(registrar(registroCon("1098765432", "CC")).getResponse().getStatus()).isEqualTo(201);
        Usuario creada = porCorreo("nueva@sena.edu.co");
        assertThat(creada.getDocumento()).isEqualTo("1098765432");
        assertThat(creada.getTipoDocumento()).isEqualTo("CC");
    }

    @Test
    void registroConDocumentoInvalido() throws Exception {
        assertThat(registrar(registroCon("99", "CC")).getResponse().getStatus()).isEqualTo(400);
        assertThat(porCorreo("nueva@sena.edu.co")).isNull();
    }

    // Muchos aprendices son menores de edad y entran con TI, no con cédula
    @Test
    void unaTarjetaDeIdentidadDeMenor() throws Exception {
        assertThat(registrar(registroCon("1012345678", "TI")).getResponse().getStatus()).isEqualTo(201);
        assertThat(porCorreo("nueva@sena.edu.co").getTipoDocumento()).isEqualTo("TI");
    }
}
