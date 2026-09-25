package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.exception.DatoInvalidoException;
import co.sena.adso.porteria.service.AuthService;
import co.sena.adso.porteria.service.CuentaService;
import co.sena.adso.porteria.service.JwtService;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

// Portería 2: tests/modulos/test_seguridad.py
class SeguridadTest extends PruebaIntegracion {

    @Autowired
    private AuthService authService;

    @Autowired
    private Clock reloj;

    @Autowired
    private JwtService jwtService;

    private void entradaDe(Usuario persona, Object fecha) {
        jdbc.update("INSERT INTO accesos (punto_id, referencia_id, tipo_referencia, tipo, fecha) "
                + "VALUES (1, ?, 'Usuario', 'Entrada', CAST(? AS TIMESTAMP))", persona.getId(), fecha);
    }

    private List<String> fechasDelPanel(Sesion celador, String desde, String hasta) throws Exception {
        JsonNode pagina = leer(mvc.perform(con(celador, get("/api/porteria/panel/accesos")
                .param("desde", desde).param("hasta", hasta))).andExpect(status().isOk()).andReturn());
        List<String> fechas = new ArrayList<>();
        pagina.get("content").forEach(fila -> fechas.add(fila.get("fecha").asText()));
        return fechas;
    }

    @Test
    void rechazaVacia() {
        assertThatThrownBy(() -> authService.validarContrasena("", null)).isInstanceOf(DatoInvalidoException.class);
        assertThatThrownBy(() -> authService.validarContrasena(null, null)).isInstanceOf(DatoInvalidoException.class);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"1234", "abc", "Abc123", "1234567"})
    void rechazaMenosDeOcho(String corta) {
        assertThatThrownBy(() -> authService.validarContrasena(corta, null)).isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    void rechazaSoloNumerosOSoloLetras() {
        assertThatThrownBy(() -> authService.validarContrasena("123456789", null)).isInstanceOf(DatoInvalidoException.class);
        assertThatThrownBy(() -> authService.validarContrasena("abcdefghi", null)).isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    void rechazaConfirmacionDistinta() {
        assertThatThrownBy(() -> authService.validarContrasena("Segura2026", "Otra2026x"))
                .isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    void aceptaValida() {
        authService.validarContrasena("Segura2026", "Segura2026");
    }

    // comparar_codigo protege el token de sesión: en la API es el sid del JWT contra el guardado en la base
    @Test
    void iguales() throws Exception {
        Sesion sesion = entrarComo(Perfil.APRENDIZ);
        mvc.perform(con(sesion, get("/api/auth/yo"))).andExpect(status().isOk());
    }

    @Test
    void distintos() throws Exception {
        Sesion sesion = entrarComo(Perfil.APRENDIZ);
        jdbc.update("UPDATE usuarios SET session_token = ? WHERE id = ?", "0".repeat(64), sesion.usuario().getId());
        mvc.perform(con(sesion, get("/api/auth/yo"))).andExpect(status().isUnauthorized());
    }

    @Test
    void vaciosNoCoinciden() throws Exception {
        Sesion sesion = entrarComo(Perfil.APRENDIZ);
        assertThat(yoConSesiones(sesion, null, "")).isEqualTo(401);
        assertThat(yoConSesiones(sesion, "", "")).isEqualTo(401);
        assertThat(yoConSesiones(sesion, null, "123456")).isEqualTo(401);
    }

    private int yoConSesiones(Sesion sesion, String guardada, String recibida) throws Exception {
        Usuario usuario = sesion.usuario();
        usuario.setSessionToken(recibida);
        String token = jwtService.generar(usuario);
        jdbc.update("UPDATE usuarios SET session_token = ? WHERE id = ?", guardada, usuario.getId());
        return mvc.perform(con(new Sesion(usuario, token), get("/api/auth/yo"))).andReturn().getResponse().getStatus();
    }

    @Test
    void quitaEtiquetas() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        String nombre = leer(mvc.perform(conJson(aprendiz, post("/api/equipos"),
                        Map.of("nombre", "<script>alert(1)</script>hola", "tipo", "Portátil")))
                .andExpect(status().isCreated()).andReturn()).get("nombre").asText();
        assertThat(nombre).doesNotContain("<script>");
    }

    @Test
    void conservaTextoNormal() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        mvc.perform(conJson(aprendiz, post("/api/equipos"), Map.of("nombre", "Portatil Dell", "tipo", "Portátil")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Portatil Dell"));
    }

    @Test
    void parseaDatetimeSinDesplazar() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        entradaDe(crearUsuario(), Timestamp.valueOf(LocalDateTime.of(2026, 8, 29, 7, 30, 0)));
        assertThat(fechasDelPanel(celador, "2026-08-29", "2026-08-29")).containsExactly("2026-08-29T07:30:00");
    }

    @Test
    void parseaCadenaDeSqlite() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Usuario persona = crearUsuario();
        entradaDe(persona, "2026-08-29 07:30:00");
        entradaDe(persona, "2026-08-29 07:30:00.123456");
        assertThat(fechasDelPanel(celador, "2026-08-29", "2026-08-29"))
                .hasSize(2).allSatisfy(f -> assertThat(f).startsWith("2026-08-29T07:30:00"));
    }

    @Test
    void devuelveNoneSiNoPuede() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        mvc.perform(con(celador, get("/api/porteria/panel/accesos"))).andExpect(status().isOk());
        mvc.perform(con(celador, get("/api/porteria/panel/accesos").param("desde", "no es una fecha")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unaEntradaDeLaMananaNoCaeAlDiaAnterior() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        entradaDe(crearUsuario(), Timestamp.valueOf(LocalDateTime.of(2026, 8, 29, 3, 0, 0)));
        assertThat(fechasDelPanel(celador, "2026-08-29", "2026-08-29")).hasSize(1);
        assertThat(fechasDelPanel(celador, "2026-08-28", "2026-08-28")).isEmpty();
    }

    @Test
    void horaColombiaEsNaive() {
        assertThat(reloj.getZone()).isEqualTo(ZoneId.of("America/Bogota"));
        assertThat(LocalDateTime.now(reloj)).isInstanceOf(LocalDateTime.class);
    }

    @Test
    void listaVaciaPermiteTodo() {
        assertThat(CuentaService.correoPermitido("cualquiera@gmail.com", List.of())).isTrue();
    }

    @Test
    void filtraPorDominio() {
        List<String> permitidos = List.of("sena.edu.co", "soy.sena.edu.co");
        assertThat(CuentaService.correoPermitido("juan@sena.edu.co", permitidos)).isTrue();
        assertThat(CuentaService.correoPermitido("ana@soy.sena.edu.co", permitidos)).isTrue();
        assertThat(CuentaService.correoPermitido("otro@gmail.com", permitidos)).isFalse();
    }

    // "sena.edu.co.atacante.com" no debe pasar como dominio del SENA
    @Test
    void noSeEnganyaConSubcadenas() {
        assertThat(CuentaService.correoPermitido("x@sena.edu.co.atacante.com", List.of("sena.edu.co"))).isFalse();
    }
}
