package co.sena.adso.porteria.soporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import co.sena.adso.porteria.config.LimitePeticionesFilter;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.repository.RolRepository;
import co.sena.adso.porteria.repository.UsuarioRepository;
import co.sena.adso.porteria.service.CaptchaService;
import co.sena.adso.porteria.service.CuentaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

// Equivale a tests/conftest.py de Portería 2: base limpia, limitador en cero y ayudas para crear y entrar
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class PruebaIntegracion {

    public static final String CLAVE = "Segura2026";

    // Un solo PostgreSQL para toda la corrida; se arranca una vez y lo reutilizan todas las clases
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LimitePeticionesFilter limitador;

    @Autowired
    private CuentaService cuentaService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private Clock relojApi;

    @Value("${app.fotos.carpeta}")
    private String carpetaFotos;

    public record Sesion(Usuario usuario, String token) {
    }

    @BeforeEach
    void baseLimpia() throws IOException {
        jdbc.execute("TRUNCATE accesos, equipos, visitantes, vehiculos, objetos_externos, auditoria, usuarios, "
                + "asistencia_clases, mensajes, fichas RESTART IDENTITY CASCADE");
        // Las mismas fichas que siembra V1
        jdbc.execute("""
                INSERT INTO fichas (numero, programa, fecha_finalizacion) VALUES
                ('2977385', 'Análisis y Desarrollo de Software', '2027-06-30'),
                ('3235642', 'Análisis y Desarrollo de Software', '2027-12-15'),
                ('2890114', 'Gestión Contable y de Información Financiera', '2026-12-10')""");
        reiniciarLimitador();
        ((Map<?, ?>) ReflectionTestUtils.getField(cuentaService, "codigosPorCorreo")).clear();
        ((Map<?, ?>) ReflectionTestUtils.getField(captchaService, "usados")).clear();
        ReflectionTestUtils.setField(captchaService, "activo", false);
        borrarFotos();
    }

    protected void reiniciarLimitador() {
        ((Map<?, ?>) ReflectionTestUtils.getField(limitador, "registros")).clear();
    }

    private void borrarFotos() throws IOException {
        Path carpeta = Path.of(carpetaFotos);
        if (!Files.exists(carpeta)) {
            return;
        }
        try (Stream<Path> rutas = Files.walk(carpeta)) {
            rutas.sorted(Comparator.reverseOrder()).filter(r -> !r.equals(carpeta)).forEach(r -> r.toFile().delete());
        }
    }

    protected Usuario crearUsuario(String correo, String cargo, String rol, String documento, Consumer<Usuario> ajustes) {
        Usuario u = new Usuario("Persona de Prueba", correo, passwordEncoder.encode(CLAVE),
                rolRepository.findByNombre(rol).orElseThrow(), cargo);
        u.setDocumento(documento);
        u.setCorreoVerificado(true);
        u.setPerfilCompleto(true);
        ajustes.accept(u);
        return usuarioRepository.save(u);
    }

    protected Usuario crearUsuario(String correo, String documento) {
        return crearUsuario(correo, "Aprendiz", "Usuario", documento, u -> { });
    }

    protected Usuario crearUsuario() {
        return crearUsuario("aprendiz@sena.edu.co", "123456");
    }

    // Aprendiz de la ficha 2758291, la que usan las vistas de formación
    protected Usuario aprendizDeFicha() {
        return crearUsuario("aprendiz.ficha@sena.edu.co", "Aprendiz", "Usuario", "3000000001", u -> {
            u.setNombre("Aprendiz Buscado");
            u.setFicha("2758291");
            u.setPrograma("Sistemas");
        });
    }

    // Como Acceso(punto_id=1, tipo='Entrada') de pytest, con la hora de Colombia que usa la API
    protected void registrarEntradaHoy(Usuario persona) {
        jdbc.update("INSERT INTO accesos (punto_id, referencia_id, tipo_referencia, tipo, fecha) "
                + "VALUES (1, ?, 'Usuario', 'Entrada', ?)", persona.getId(), LocalDateTime.now(relojApi));
    }

    protected Sesion entrarComo(Perfil perfil) throws Exception {
        return entrarComo(perfil, u -> { });
    }

    protected Sesion entrarComo(Perfil perfil, Consumer<Usuario> ajustes) throws Exception {
        Usuario u = crearUsuario(perfil.correo(), perfil.cargo, perfil.rol, perfil.documento, usuario -> {
            usuario.setNombre(perfil.nombre);
            ajustes.accept(usuario);
        });
        return new Sesion(u, iniciarSesion(u.getCorreo(), CLAVE));
    }

    protected String iniciarSesion(String identificador, String clave) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(aJson(Map.of("identificador", identificador, "password", clave))))
                .andReturn();
        assertThat(r.getResponse().getStatus()).as("login de prueba").isEqualTo(200);
        return leer(r).get("token").asText();
    }

    protected MockHttpServletRequestBuilder con(Sesion sesion, MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + sesion.token());
    }

    protected MockHttpServletRequestBuilder conJson(Sesion sesion, MockHttpServletRequestBuilder peticion, Object cuerpo)
            throws Exception {
        return con(sesion, peticion).contentType(MediaType.APPLICATION_JSON).content(aJson(cuerpo));
    }

    protected String aJson(Object cuerpo) throws Exception {
        return json.writeValueAsString(cuerpo);
    }

    protected JsonNode leer(MvcResult resultado) throws Exception {
        return json.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    // Mismos datos base que el registro de las pruebas de Portería 2
    protected Map<String, Object> datosRegistro() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", "Persona Nueva");
        datos.put("correo", "nueva@sena.edu.co");
        datos.put("documento", "999888");
        datos.put("password", CLAVE);
        datos.put("confirmacion", CLAVE);
        datos.put("aceptaDatos", true);
        return datos;
    }

    protected MvcResult registrar(Map<String, Object> datos) throws Exception {
        return mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(aJson(datos))).andReturn();
    }

    protected MvcResult pedirCodigo(String correo) throws Exception {
        return mvc.perform(post("/api/auth/recuperacion").contentType(MediaType.APPLICATION_JSON)
                .content(aJson(Map.of("correo", correo)))).andReturn();
    }

    protected Usuario porCorreo(String correo) {
        return usuarioRepository.buscarPorCorreoODocumento(correo).orElse(null);
    }
}
