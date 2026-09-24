package co.sena.adso.porteria.vistas.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Rol;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

// Portería 2: tests/vistas/login/test_login.py
class LoginTest extends PruebaIntegracion {

    record Datos(String id, String rol, String cargo, String correo, String documento, String nombre) {
        @Override
        public String toString() {
            return id;
        }
    }

    static final List<Datos> USUARIOS = List.of(
            new Datos("Admin", Rol.ADMIN, "Administrador", "admin@sena.edu.co", "1000000001", "Admin de Prueba"),
            new Datos("Administrador", Rol.USUARIO, "Administrador", "administrador@sena.edu.co", "1000000002", "Administrador de Prueba"),
            new Datos("Aprendiz", Rol.USUARIO, "Aprendiz", "aprendiz@sena.edu.co", "1000000003", "Aprendiz de Prueba"),
            new Datos("Instructor", Rol.USUARIO, "Instructor", "instructor@sena.edu.co", "1000000004", "Instructor de Prueba"),
            new Datos("Administrativo", Rol.USUARIO, "Administrativo", "administrativo@sena.edu.co", "1000000005", "Administrativo de Prueba"),
            new Datos("Celador", Rol.USUARIO, "Celador", "celador@sena.edu.co", "1000000006", "Celador de Prueba"),
            new Datos("Portería", Rol.USUARIO, "Portería", "porteria@sena.edu.co", "1000000007", "Portería de Prueba"),
            new Datos("Coordinacion", Rol.USUARIO, "Coordinacion", "coordinacion@sena.edu.co", "1000000008", "Coordinacion de Prueba"),
            new Datos("Subdirector", Rol.USUARIO, "Subdirector", "subdirector@sena.edu.co", "1000000009", "Subdirector de Prueba"),
            new Datos("Contratista", Rol.USUARIO, "Contratista", "contratista@sena.edu.co", "1000000010", "Contratista de Prueba"),
            new Datos("Funcionario", Rol.USUARIO, "Funcionario", "funcionario@sena.edu.co", "1000000011", "Funcionario de Prueba"));

    static List<Datos> usuarios() {
        return USUARIOS;
    }

    private Usuario nuevo(Datos d) {
        return crearUsuario(d.correo(), d.cargo(), d.rol(), d.documento(), u -> u.setNombre(d.nombre()));
    }

    private ResultActions login(String identificador, String contrasena) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(aJson(Map.of("identificador", identificador, "password", contrasena))));
    }

    private Usuario recargar(Usuario u) {
        return usuarioRepository.findById(u.getId()).orElseThrow();
    }

    @Test
    void cubreTodosLosCargos() {
        assertThat(USUARIOS.stream().filter(d -> Rol.USUARIO.equals(d.rol())).map(Datos::cargo).collect(Collectors.toSet()))
                .isEqualTo(Set.copyOf(Usuario.CARGOS_VALIDOS));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("usuarios")
    void entraConCorreo(Datos datos) throws Exception {
        nuevo(datos);
        login(datos.correo(), CLAVE).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("usuarios")
    void entraConDocumento(Datos datos) throws Exception {
        nuevo(datos);
        login(datos.documento(), CLAVE).andExpect(jsonPath("$.token").isNotEmpty());
    }

    // La API no redirige: entrega el token y el usuario, y React lleva a la persona a su página
    @ParameterizedTest(name = "{0}")
    @MethodSource("usuarios")
    void formularioSinAjaxRedirige(Datos datos) throws Exception {
        Usuario u = nuevo(datos);
        login(datos.correo(), CLAVE).andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.id").value(u.getId()));
    }

    // React no muestra el login a quien ya tiene sesión: lo decide con /api/auth/yo
    @ParameterizedTest(name = "{0}")
    @MethodSource("usuarios")
    void yaAutenticadoNoVuelveAVerElLogin(Datos datos) throws Exception {
        nuevo(datos);
        String token = iniciarSesion(datos.correo(), CLAVE);
        mvc.perform(get("/api/auth/yo").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(datos.correo()));
    }

    // La página la sirve React; lo que debe estar abierto sin sesión es el endpoint de login
    @Test
    void laPaginaDeLoginAbre() throws Exception {
        login("nadie@sena.edu.co", "Incorrecta2026").andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("usuarios")
    void correoSinImportarMayusculasNiEspacios(Datos datos) throws Exception {
        nuevo(datos);
        login("  " + datos.correo().toUpperCase() + " ", CLAVE).andExpect(jsonPath("$.token").isNotEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("usuarios")
    void contrasenaIncorrectaNoEntra(Datos datos) throws Exception {
        Usuario usuario = nuevo(datos);
        login(datos.correo(), "Incorrecta2026").andExpect(status().isUnauthorized());
        assertThat(recargar(usuario).getIntentosFallidos()).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("usuarios")
    void bloqueoTrasCincoIntentos(Datos datos) throws Exception {
        Usuario usuario = nuevo(datos);
        for (int i = 0; i < 5; i++) {
            login(datos.correo(), "Incorrecta2026");
        }
        assertThat(recargar(usuario).getBloqueadoHasta()).isNotNull();
        MvcResult r = login(datos.correo(), CLAVE).andExpect(status().isForbidden()).andReturn();
        assertThat(leer(r).get("bloqueadoSegundos").asLong()).isGreaterThan(0);
    }

    @Test
    void noRevelaSiLaCuentaExiste() throws Exception {
        crearUsuario("ana@sena.edu.co", "123456");
        MvcResult existente = login("ana@sena.edu.co", "ContrasenaMala1").andReturn();
        MvcResult inexistente = login("nadie@sena.edu.co", "ContrasenaMala1").andReturn();
        assertThat(leer(existente).get("mensaje")).isEqualTo(leer(inexistente).get("mensaje"));
        assertThat(existente.getResponse().getStatus()).isEqualTo(inexistente.getResponse().getStatus());
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

    // Flask respondía 401; la API usa 400 porque la validación rechaza el cuerpo antes de intentar el login
    @ParameterizedTest(name = "{0}")
    @CsvSource(value = {"sin-contrasena, ana@sena.edu.co, ''", "sin-identificador, '', Segura2026", "vacio, '', ''"})
    void camposVaciosNoEntran(String caso, String identificador, String contrasena) throws Exception {
        crearUsuario("ana@sena.edu.co", "123456");
        login(identificador, contrasena).andExpect(status().isBadRequest()).andExpect(jsonPath("$.token").doesNotExist());
    }
}
