package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Rol;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.exception.DatoInvalidoException;
import co.sena.adso.porteria.repository.FichaRepository;
import co.sena.adso.porteria.service.CarnetService;
import co.sena.adso.porteria.service.CodigoBarrasService;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

// Portería 2: tests/modulos/test_carnet.py
class CarnetTest extends PruebaIntegracion {

    private final CarnetService carnet = conPerfiles("");

    // El primer parámetro es el texto de CARNET_PERFILES
    private static CarnetService conPerfiles(String perfiles) {
        return new CarnetService(perfiles, "Regional Santander", "Centro de Gestión Agroempresarial del Oriente",
                "Aseguradora Aurora", "601-7443718 Op. 1", "100603", new CodigoBarrasService());
    }

    @Autowired
    private FichaRepository fichaRepository;

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "Aprendiz, APRENDIZ",
            "Instructor, INSTRUCTOR",
            "Celador, CONTRATISTA",
            "Portería, CONTRATISTA",
            "Administrativo, FUNCIONARIO",
            "Administrador, FUNCIONARIO"})
    void cadaCargoActualTienePerfil(String cargo, String perfil) {
        assertThat(carnet.perfilDeCargo(cargo)).isEqualTo(perfil);
    }

    @Test
    void unCargoDesconocidoNoRevienta() {
        List<String> validos = List.of(CarnetService.APRENDIZ, CarnetService.INSTRUCTOR, CarnetService.CONTRATISTA,
                CarnetService.FUNCIONARIO, CarnetService.SUBDIRECTOR);
        assertThat(carnet.perfilDeCargo("Cargo Que No Existe")).isIn(validos);
        assertThat(carnet.perfilDeCargo(null)).isIn(validos);
        assertThat(carnet.perfilDeCargo("")).isIn(validos);
    }

    @Test
    void noDistingueMayusculas() {
        assertThat(carnet.perfilDeCargo("APRENDIZ")).isEqualTo(CarnetService.APRENDIZ);
        assertThat(carnet.perfilDeCargo("  instructor ")).isEqualTo(CarnetService.INSTRUCTOR);
    }

    @Test
    void sePuedeReasignarPorVariableDeEntorno() {
        assertThat(conPerfiles("Celador:FUNCIONARIO").perfilDeCargo("Celador")).isEqualTo(CarnetService.FUNCIONARIO);
    }

    @Test
    void unValorMalEscritoSeIgnora() {
        assertThat(conPerfiles("Celador:INVENTADO,basura,:").perfilDeCargo("Celador"))
                .isEqualTo(CarnetService.CONTRATISTA);
    }

    @Test
    void elPerfilNoCambiaLosPermisos() {
        Usuario celador = crearUsuario("celador@sena.edu.co", "Celador", Rol.USUARIO, "500", u -> { });
        assertThat(carnet.perfilDeCargo(celador.getCargo())).isEqualTo(CarnetService.CONTRATISTA);
        assertThat(celador.puedeOperarPorteria()).isTrue();
        assertThat(celador.puedeAsesorar()).isFalse();
    }

    @Test
    void siLosDeclaroSeUsanTalCual() {
        assertThat(carnet.partirNombre("Cualquier Cosa", "María José", "De La Cruz"))
                .containsExactly("María José", "De La Cruz");
    }

    @Test
    void cuatroPalabrasSePartenPorLaMitad() {
        assertThat(carnet.partirNombre("Juan Carlos Pérez Gómez", null, null))
                .containsExactly("Juan Carlos", "Pérez Gómez");
    }

    @Test
    void tresPalabrasVanUnoYDos() {
        assertThat(carnet.partirNombre("Juan Pérez Gómez", null, null)).containsExactly("Juan", "Pérez Gómez");
    }

    @Test
    void dosPalabras() {
        assertThat(carnet.partirNombre("Ana Torres", null, null)).containsExactly("Ana", "Torres");
    }

    @Test
    void unaSolaPalabraNoInventaApellido() {
        assertThat(carnet.partirNombre("Cher", null, null)).containsExactly("Cher", "");
    }

    @Test
    void cincoPalabrasDejanLoSobranteEnApellidos() {
        assertThat(carnet.partirNombre("Ana María Del Río Vargas", null, null))
                .containsExactly("Ana María", "Del Río Vargas");
    }

    @Test
    void vacioNoRevienta() {
        assertThat(carnet.partirNombre("", null, null)).containsExactly("", "");
        assertThat(carnet.partirNombre(null, null, null)).containsExactly("", "");
    }

    @Test
    void losCamposDeclaradosMandanSobreElReparto() throws Exception {
        Sesion persona = entrar("persona@sena.edu.co", "Aprendiz", "1",
                u -> u.setNombre("Juan Carlos Pérez Gómez"));
        assertThat(verCarnet(persona).get("nombres").asText()).isEqualTo("Juan Carlos");

        mvc.perform(conJson(persona, put("/api/perfil"), Map.of("nombres", "Juan", "apellidos", "Carlos Pérez Gómez")))
                .andExpect(status().isOk());
        JsonNode datos = verCarnet(persona);
        assertThat(datos.get("nombres").asText()).isEqualTo("Juan");
        assertThat(datos.get("apellidos").asText()).isEqualTo("Carlos Pérez Gómez");
    }

    @Test
    void abreviaturas() {
        assertThat(carnet.construir(conTipo("CC")).documento()).isEqualTo("C.C. 1");
        assertThat(carnet.construir(conTipo("TI")).documento()).isEqualTo("T.I. 1");
        // Sin tipo declarado la entidad trae CC por defecto (la columna no admite nulos)
        Usuario sinTipo = crearUsuario("sintipo@sena.edu.co", "1");
        assertThat(carnet.construir(sinTipo).documento()).startsWith("C.C. ");
    }

    @Test
    void elCarnetMuestraTipoYNumero() throws Exception {
        Sesion persona = entrar("persona@sena.edu.co", "Aprendiz", "1098765432", u -> u.setTipoDocumento("TI"));
        assertThat(verCarnet(persona).get("documento").asText()).isEqualTo("T.I. 1098765432");
    }

    @Test
    void sinDocumentoNoInventaNada() throws Exception {
        Sesion persona = entrar("persona@sena.edu.co", "Aprendiz", null, u -> { });
        assertThat(verCarnet(persona).get("documento").asText()).isEmpty();
    }

    @Test
    void elCarnetDeAprendizLlevaFichaProgramaFechaYPoliza() throws Exception {
        Sesion persona = preparar("Aprendiz",
                u -> u.asignarFicha(fichaRepository.findByNumero("2977385").orElseThrow()));
        JsonNode datos = verCarnet(persona);

        assertThat(datos.get("perfil").asText()).isEqualTo("APRENDIZ");
        assertThat(datos.get("nombres").asText()).isEqualTo("María José");
        assertThat(datos.get("apellidos").asText()).isEqualTo("De La Cruz");
        assertThat(datos.get("documento").asText()).isEqualTo("C.C. 1098765432");
        assertThat(datos.get("ficha").asText()).isEqualTo("2977385");
        assertThat(datos.get("fechaFinalizacion").asText()).isEqualTo("30/06/2027");
        assertThat(datos.get("programa").asText()).isEqualTo("Análisis y Desarrollo de Software");
        assertThat(datos.get("aseguradora").asText()).isEqualTo("Aseguradora Aurora");
        assertThat(datos.get("poliza").asText()).isEqualTo("100603");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"Instructor, INSTRUCTOR", "Celador, CONTRATISTA", "Administrativo, FUNCIONARIO"})
    void losDemasPerfilesNoLlevanFichaNiPoliza(String cargo, String perfil) throws Exception {
        JsonNode datos = verCarnet(preparar(cargo, u -> { }));

        assertThat(datos.get("perfil").asText()).isEqualTo(perfil);
        assertThat(datos.get("documento").asText()).isEqualTo("C.C. 1098765432");
        assertThat(datos.get("tipoSangre").asText()).isEqualTo("O+");
        assertThat(datos.get("ficha").isNull()).isTrue();
        assertThat(datos.get("fechaFinalizacion").asText()).isEmpty();
        assertThat(datos.get("aseguradora").isNull()).isTrue();
        assertThat(datos.get("poliza").isNull()).isTrue();
    }

    @Test
    void todosLosPerfilesLlevanCodigoDeBarras() throws Exception {
        JsonNode datos = verCarnet(preparar("Instructor", u -> { }));
        assertThat(datos.get("activo").asBoolean()).isTrue();
        assertThat(datos.get("codigoBarras").asText()).isEqualTo("1098765432");
        assertThat(datos.get("codigoBarrasSvg").asText()).startsWith("<svg");
    }

    @Test
    void sinPerfilCompletoNoHayCodigo() throws Exception {
        Sesion persona = entrar("persona@sena.edu.co", "Aprendiz", "1010", u -> u.setPerfilCompleto(false));
        JsonNode datos = verCarnet(persona);
        assertThat(datos.get("activo").asBoolean()).isFalse();
        assertThat(datos.get("codigoBarras").isNull()).isTrue();
    }

    @Test
    void noQuedanEscritasAFuegoEnLasPlantillas() throws IOException {
        List<String> prohibidas = List.of("Regional Santander", "Centro de Gestión Agroempresarial del Oriente",
                "Aseguradora Aurora", "100603");
        List<String> encontradas = new ArrayList<>();
        // application.properties es la configuración (el config.py de Portería 2): ahí sí van los valores por defecto
        try (Stream<Path> rutas = Files.walk(Path.of("src/main"))) {
            for (Path ruta : rutas.filter(Files::isRegularFile)
                    .filter(r -> !r.getFileName().toString().startsWith("application")).toList()) {
                String contenido = Files.readString(ruta, StandardCharsets.UTF_8);
                prohibidas.stream().filter(contenido::contains)
                        .forEach(p -> encontradas.add(ruta.getFileName() + ": " + p));
            }
        }
        assertThat(encontradas).as("generalidades escritas dentro de src/main").isEmpty();
    }

    @Test
    void estanDisponiblesEnTodasLasPlantillas() throws Exception {
        JsonNode datos = verCarnet(entrar("persona@sena.edu.co", "Aprendiz", "1010", u -> { }));
        assertThat(datos.get("regional").asText()).isEqualTo("Regional Santander");
        assertThat(datos.get("centro").asText()).isEqualTo("Centro de Gestión Agroempresarial del Oriente");
    }

    @Test
    void laTablaDePatronesEsValida() {
        List<String> patrones = CodigoBarrasService.PATRONES;
        assertThat(patrones).hasSize(107).doesNotHaveDuplicates();
        for (int valor = 0; valor < 106; valor++) {
            String patron = patrones.get(valor);
            assertThat(patron).as("valor %d", valor).hasSize(6);
            assertThat(patron.chars().map(c -> c - '0').sum()).as("valor %d", valor).isEqualTo(11);
            // Propiedad del Code128: los tres anchos de barra suman par
            assertThat((patron.charAt(0) - '0' + patron.charAt(2) - '0' + patron.charAt(4) - '0') % 2)
                    .as("valor %d", valor).isZero();
        }
        assertThat(patrones.get(106)).as("el patrón de parada cambió").isEqualTo("2331112");
    }

    @Test
    void elDigitoDeControlEsElEsperado() {
        List<Integer> valores = new CodigoBarrasService().valores("12345");
        assertThat(valores.get(0)).as("debe empezar con Start B").isEqualTo(104);
        assertThat(valores.subList(1, 6)).containsExactly(17, 18, 19, 20, 21);
        assertThat(valores.get(valores.size() - 2)).isEqualTo(90);
        assertThat(valores.get(valores.size() - 1)).as("debe terminar con Stop").isEqualTo(106);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"1098765432", "SENA-VISIT:1098765432", "SENA-VEH-S:ABC123", "SENA-VEH-E:ABC123",
            "SENA-OBJ:SN-1756512345"})
    void codificaLosCodigosRealesDelSistema(String dato) {
        String svg = new CodigoBarrasService().svg(dato);
        assertThat(svg).startsWith("<svg").contains("fill=\"#000\"").contains("fill=\"#fff\"");
    }

    @Test
    void elSvgEscalaAlAnchoDelContenedor() {
        String svg = new CodigoBarrasService().svg("123456");
        assertThat(svg).contains("preserveAspectRatio=\"none\"").contains("viewBox=");
        assertThat(svg.split("<rect")[0]).as("el <svg> no debe fijar ancho").doesNotContain("width=\"");
    }

    @Test
    void unCaracterNoRepresentableSeRechaza() {
        CodigoBarrasService codigo = new CodigoBarrasService();
        assertThatThrownBy(() -> codigo.svg("camión")).isInstanceOf(DatoInvalidoException.class);
        assertThatThrownBy(() -> codigo.svg("")).isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    void elSimboloCrece11ModulosPorCaracter() {
        CodigoBarrasService codigo = new CodigoBarrasService();
        assertThat(codigo.modulos("AB")).hasSize(codigo.modulos("A").size() + 6);
    }

    // Otro contexto de Spring con las generalidades cambiadas, como si fuera otro .env
    @Nested
    @TestPropertySource(properties = {"app.carnet.regional=Regional Antioquia", "app.carnet.centro=Centro de Comercio"})
    class OtroCentro extends PruebaIntegracion {

        @Test
        void cambiarlasCambiaLoQueSeImprime() throws Exception {
            crearUsuario("persona@sena.edu.co", "Aprendiz", Rol.USUARIO, "1010", u -> { });
            Sesion persona = new Sesion(null, iniciarSesion("persona@sena.edu.co", CLAVE));
            String cuerpo = mvc.perform(con(persona, get("/api/perfil/carnet"))).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(cuerpo).contains("Regional Antioquia").doesNotContain("Regional Santander");
        }
    }

    private Sesion preparar(String cargo, Consumer<Usuario> extra) throws Exception {
        return entrar("persona@sena.edu.co", cargo, "1098765432", u -> {
            u.setTipoSangre("O+");
            u.setNombres("María José");
            u.setApellidos("De La Cruz");
            u.registrarFotoNueva("foto.jpg", LocalDateTime.now());
            u.revisarFoto(true, null, null, LocalDateTime.now());
            extra.accept(u);
        });
    }

    private Sesion entrar(String correo, String cargo, String documento, Consumer<Usuario> ajustes) throws Exception {
        Usuario u = crearUsuario(correo, cargo, Rol.USUARIO, documento, ajustes);
        return new Sesion(u, iniciarSesion(correo, CLAVE));
    }

    private JsonNode verCarnet(Sesion sesion) throws Exception {
        return leer(mvc.perform(con(sesion, get("/api/perfil/carnet"))).andExpect(status().isOk()).andReturn());
    }

    private static Usuario conTipo(String tipo) {
        Usuario u = mock(Usuario.class);
        when(u.getDocumento()).thenReturn("1");
        when(u.getTipoDocumento()).thenReturn(tipo);
        return u;
    }
}
