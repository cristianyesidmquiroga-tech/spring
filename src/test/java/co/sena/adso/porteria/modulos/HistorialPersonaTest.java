package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Rol;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.service.HistorialService;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// Portería 2: tests/modulos/test_historial_persona.py
class HistorialPersonaTest extends PruebaIntegracion {

    private static final String RUTA = "/api/historial";
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    // Fecha fija: sin ella cada prueba mediría un periodo distinto según el día en que corra
    private static final LocalDate HOY = LocalDate.of(2026, 9, 1);
    private static final String FICHA = "2758899";

    @MockBean
    private Clock reloj;

    private Sesion celador;

    @BeforeEach
    void fijarHoy() {
        when(reloj.getZone()).thenReturn(BOGOTA);
        when(reloj.instant()).thenReturn(HOY.atTime(10, 0).atZone(BOGOTA).toInstant());
    }

    // --- Permisos ---

    @Test
    void aprendizVeSuPropioHistorial() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        mvc.perform(con(aprendiz, get(RUTA).param("usuarioId", id(aprendiz.usuario()))))
                .andExpect(status().isOk());
    }

    @Test
    void aprendizNoVeElHistorialDeOtro() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        Usuario otro = crearUsuario("otro@sena.edu.co", "999999");
        mvc.perform(con(aprendiz, get(RUTA).param("usuarioId", id(otro))))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiNiegaCon403ElHistorialAjeno() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        Usuario otro = crearUsuario("otro@sena.edu.co", "999999");
        mvc.perform(con(aprendiz, get(RUTA).param("usuarioId", id(otro))))
                .andExpect(status().isForbidden());
    }

    @Test
    void aprendizNoPuedeListarUnaFichaCompleta() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ, u -> u.setFicha(FICHA));
        crearUsuario("otro@sena.edu.co", "Aprendiz", Rol.USUARIO, "999999", u -> u.setFicha(FICHA));
        JsonNode cuerpo = consultar(aprendiz, "ficha", FICHA);
        assertThat(cuerpo.get("personas").findValuesAsText("id")).containsExactly(id(aprendiz.usuario()));
    }

    @Test
    void celadorConsultaACualquiera() throws Exception {
        Usuario aprendiz = crearUsuario();
        JsonNode cuerpo = consultar(celador(), "usuarioId", id(aprendiz));
        assertThat(cuerpo.get("personas").get(0).get("id").asLong()).isEqualTo(aprendiz.getId());
    }

    @Test
    void instructorConsultaSuFicha() throws Exception {
        Sesion instructor = entrarComo(Perfil.INSTRUCTOR);
        crearUsuario("ap1@sena.edu.co", "Aprendiz", Rol.USUARIO, "111111", u -> u.setFicha(FICHA));
        crearUsuario("ap2@sena.edu.co", "Aprendiz", Rol.USUARIO, "222222", u -> u.setFicha(FICHA));
        assertThat(consultar(instructor, "ficha", FICHA).get("personas")).hasSize(2);
    }

    @Test
    void anonimoEsRedirigidoAlLogin() throws Exception {
        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    }

    // --- Emparejado ---

    @Test
    void entradaYSalidaSeEmparejanConPermanencia() throws Exception {
        Usuario persona = ana();
        acceso(persona, "Entrada", HOY.atTime(7, 0));
        acceso(persona, "Salida", HOY.atTime(12, 30));

        JsonNode movimientos = bloque(persona, HOY, HOY).get("movimientos");
        assertThat(movimientos).hasSize(1);
        assertThat(movimientos.get(0).get("permanenciaMinutos").asLong()).isEqualTo(330);
        assertThat(momento(movimientos.get(0).get("salida")).getHour()).isEqualTo(12);
    }

    @Test
    void entradaDeHoySinSalidaEsAlguienQueSigueAdentro() throws Exception {
        Usuario persona = ana();
        acceso(persona, "Entrada", HOY.atTime(8, 0));

        JsonNode bloque = bloque(persona, HOY, HOY);
        assertThat(bloque.get("movimientos")).hasSize(1);
        assertThat(bloque.get("movimientos").get(0).get("salida").isNull()).isTrue();
        assertThat(bloque.get("movimientos").get(0).get("abierto").asBoolean()).isTrue();
        // No es anomalía: todavía no le toca salir
        assertThat(bloque.get("resumen").get("sinSalida").asInt()).isZero();
        assertThat(bloque.get("resumen").get("abiertos").asInt()).isEqualTo(1);
    }

    @Test
    void entradaAntiguaSinSalidaSiEsAnomalia() throws Exception {
        Usuario persona = ana();
        LocalDate dia = HOY.minusDays(10);
        acceso(persona, "Entrada", dia.atTime(8, 0));

        JsonNode resumen = bloque(persona, dia, dia).get("resumen");
        assertThat(resumen.get("sinSalida").asInt()).isEqualTo(1);
        assertThat(resumen.get("abiertos").asInt()).isZero();
    }

    @Test
    void dosEntradasSeguidasDejanLaPrimeraSinSalida() throws Exception {
        Usuario persona = ana();
        acceso(persona, "Entrada", HOY.atTime(7, 0));
        acceso(persona, "Entrada", HOY.atTime(9, 0));
        acceso(persona, "Salida", HOY.atTime(11, 0));

        JsonNode bloque = bloque(persona, HOY, HOY);
        JsonNode movimientos = bloque.get("movimientos");
        assertThat(movimientos).hasSize(2);
        assertThat(movimientos.get(0).get("salida").isNull()).isTrue();
        assertThat(movimientos.get(1).get("permanenciaMinutos").asLong()).isEqualTo(120);
        // Solo el último movimiento puede quedar abierto, aunque sea de hoy
        assertThat(bloque.get("resumen").get("sinSalida").asInt()).isEqualTo(1);
    }

    @Test
    void salidaSinEntradaPreviaSeConserva() throws Exception {
        Usuario persona = ana();
        acceso(persona, "Salida", HOY.atTime(6, 0));

        JsonNode movimientos = bloque(persona, HOY, HOY).get("movimientos");
        assertThat(movimientos).hasSize(1);
        assertThat(movimientos.get(0).get("entrada").isNull()).isTrue();
        assertThat(momento(movimientos.get(0).get("salida")).getHour()).isEqualTo(6);
        assertThat(movimientos.get(0).get("entradaFueraDeVentana").asBoolean()).isTrue();
    }

    @Test
    void equiposSeTraducenANombresYToleranBorrados() throws Exception {
        Usuario persona = ana();
        long portatil = equipo(persona, "Portatil Lenovo");
        acceso(persona, "Entrada", HOY.atTime(7, 0), "Usuario", portatil + ",9999", null);

        List<String> equipos = textos(bloque(persona, HOY, HOY).get("movimientos").get(0).get("equipos"));
        assertThat(equipos).contains("Portatil Lenovo");
        assertThat(equipos).anyMatch(nombre -> nombre.contains("9999"));
    }

    // --- Cierre automático ---

    @Test
    void cierreDeMedianocheSinOperadorSeMarca() throws Exception {
        Usuario persona = ana();
        acceso(persona, "Entrada", HOY.atTime(7, 0));
        acceso(persona, "Salida", HOY.atTime(23, 59, 59));

        JsonNode movimiento = bloque(persona, HOY, HOY).get("movimientos").get(0);
        assertThat(movimiento.get("cierreAutomatico").asBoolean()).isTrue();
    }

    @Test
    void salidaRealALas2359NoEsCierreAutomatico() throws Exception {
        Usuario operador = celador().usuario();
        Usuario persona = ana();
        acceso(persona, "Entrada", HOY.atTime(7, 0), "Usuario", null, operador.getId());
        acceso(persona, "Salida", HOY.atTime(23, 59, 59), "Usuario", null, operador.getId());

        JsonNode movimiento = bloque(persona, HOY, HOY).get("movimientos").get(0);
        assertThat(movimiento.get("cierreAutomatico").asBoolean()).isFalse();
    }

    // --- Aislamiento de entidades ---

    @Test
    void noSeMezclanAccesosDeVisitantes() throws Exception {
        Usuario persona = ana();
        acceso(persona, "Entrada", HOY.atTime(7, 0));
        // Mismo referencia_id pero otra entidad
        acceso(persona, "Entrada", HOY.atTime(8, 0), "Visitante", null, null);
        acceso(persona, "Entrada", HOY.atTime(9, 0), "Vehiculo", null, null);

        JsonNode movimientos = bloque(persona, HOY, HOY).get("movimientos");
        assertThat(movimientos).hasSize(1);
        assertThat(momento(movimientos.get(0).get("entrada")).getHour()).isEqualTo(7);
    }

    @Test
    void losAccesosNoSeCruzanEntrePersonas() throws Exception {
        Usuario ana = crearUsuario("ana@sena.edu.co", "111111");
        Usuario luis = crearUsuario("luis@sena.edu.co", "222222");
        acceso(ana, "Entrada", HOY.atTime(7, 0));
        acceso(luis, "Entrada", HOY.atTime(8, 0));
        acceso(luis, "Salida", HOY.atTime(10, 0));

        JsonNode cuerpo = consultar(celador(), "usuarioId", id(ana), "usuarioId", id(luis),
                "desde", HOY.toString(), "hasta", HOY.toString());
        Map<Long, JsonNode> datos = new HashMap<>();
        cuerpo.get("personas").forEach(b -> datos.put(b.get("id").asLong(), b));
        assertThat(datos.get(ana.getId()).get("movimientos").get(0).get("salida").isNull()).isTrue();
        assertThat(datos.get(luis.getId()).get("movimientos").get(0).get("permanenciaMinutos").asLong()).isEqualTo(120);
    }

    // --- Cortes del rango ---

    @Test
    void movimientoTotalmenteFueraDelRangoNoSeCuela() throws Exception {
        Usuario persona = ana();
        jornada(persona, LocalDate.of(2026, 8, 8));

        JsonNode bloque = bloque(persona, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 7));
        assertThat(bloque.get("movimientos")).isEmpty();
    }

    // --- Topes de carga ---

    @Test
    void rangoDemasiadoAmplioSeRecortaYSeAvisa() throws Exception {
        Sesion celador = celador();
        JsonNode cuerpo = consultar(celador, "usuarioId", id(celador.usuario()),
                "desde", "2025-01-01", "hasta", "2026-08-28");
        assertThat(cuerpo.get("rangoRecortado").asBoolean()).isTrue();
        assertThat(diasDelRango(cuerpo)).isEqualTo(maximo("MAXIMO_DIAS_RANGO"));
    }

    @Test
    void rangoNormalNoSeRecorta() throws Exception {
        Sesion celador = celador();
        JsonNode cuerpo = consultar(celador, "usuarioId", id(celador.usuario()),
                "desde", "2026-08-01", "hasta", "2026-08-28");
        assertThat(cuerpo.get("rangoRecortado").asBoolean()).isFalse();
    }

    @Test
    void excesoDeAccesosDejaPersonasFueraEnVezDeTraerlasAMedias() throws Exception {
        Usuario ana = crearUsuario("ana@sena.edu.co", "Aprendiz", Rol.USUARIO, "111111", u -> u.setNombre("Ana"));
        Usuario luis = crearUsuario("luis@sena.edu.co", "Aprendiz", Rol.USUARIO, "222222", u -> u.setNombre("Luis"));
        // El tope es una constante del servicio: se llena de verdad en vez de bajarlo
        llenarHastaElTope(ana, LocalDate.of(2026, 8, 3));
        jornada(luis, LocalDate.of(2026, 8, 3));

        JsonNode cuerpo = consultar(celador(), "usuarioId", id(ana), "usuarioId", id(luis),
                "desde", "2026-08-03", "hasta", "2026-08-07");
        assertThat(textos(cuerpo.get("omitidas"))).containsExactly("Luis");
        assertThat(cuerpo.get("personas").findValuesAsText("id")).containsExactly(id(ana));
    }

    // --- Vista (en Spring es el contrato JSON que pinta el frontend) ---

    @Test
    void sinFiltrosMuestraElFormularioVacio() throws Exception {
        JsonNode cuerpo = consultar(celador());
        assertThat(cuerpo.get("personas")).isEmpty();
    }

    @Test
    void laPaginaMuestraElNombreYLosEquipos() throws Exception {
        Sesion celador = celador();
        Usuario ana = crearUsuario("ana@sena.edu.co", "Aprendiz", Rol.USUARIO, "111111",
                u -> u.setNombre("Ana Rodriguez"));
        long equipo = equipo(ana, "Portatil Dell");
        acceso(ana, "Entrada", HOY.atTime(7, 0), "Usuario", String.valueOf(equipo), null);
        acceso(ana, "Salida", HOY.atTime(12, 0));

        JsonNode bloque = consultar(celador, "usuarioId", id(ana)).get("personas").get(0);
        assertThat(bloque.get("nombre").asText()).isEqualTo("Ana Rodriguez");
        assertThat(textos(bloque.get("movimientos").get(0).get("equipos"))).contains("Portatil Dell");
        assertThat(bloque.get("resumen").get("periodoEvaluadoInicio").isNull()).isFalse();
        assertThat(bloque.get("resumen").get("periodoEvaluadoFin").isNull()).isFalse();
    }

    @Test
    void laPaginaNoPintaUnPorcentajeSinDiasExigibles() throws Exception {
        Sesion celador = celador();
        Usuario ana = crearUsuario("ana@sena.edu.co", "Aprendiz", Rol.USUARIO, "111111",
                u -> u.setNombre("Ana Rodriguez"));
        jornada(ana, HOY);

        LocalDate futuro = HOY.plusDays(10);
        JsonNode resumen = consultar(celador, "usuarioId", id(ana), "desde", futuro.toString(),
                "hasta", futuro.plusDays(4).toString()).get("personas").get(0).get("resumen");
        assertThat(resumen.get("diasEsperados").asInt()).isZero();
        assertThat(resumen.get("porcentajeAsistencia").isNull()).isTrue();
    }

    @Test
    void laPaginaExplicaElEmpateEnVezDeAfirmarUnDia() throws Exception {
        Sesion celador = celador();
        Usuario ana = crearUsuario("ana@sena.edu.co", "Aprendiz", Rol.USUARIO, "111111",
                u -> u.setNombre("Ana Rodriguez"));
        anclarVinculacion(ana);

        JsonNode resumen = consultar(celador, "usuarioId", id(ana), "desde", "2026-08-03", "hasta", "2026-08-21")
                .get("personas").get(0).get("resumen");
        assertThat(resumen.get("diaMasFaltado").isNull()).isTrue();
        assertThat(resumen.get("motivoSinDia").asText()).isEqualTo("empate");
        assertThat(resumen.get("diasEmpatados")).isNotEmpty();
    }

    @Test
    void laPaginaAvisaCuandoRecortaElRango() throws Exception {
        Sesion celador = celador();
        JsonNode cuerpo = consultar(celador, "usuarioId", id(celador.usuario()),
                "desde", "2025-01-01", "hasta", "2026-08-28");
        assertThat(cuerpo.get("rangoRecortado").asBoolean()).isTrue();
        assertThat(diasDelRango(cuerpo)).isEqualTo(maximo("MAXIMO_DIAS_RANGO"));
    }

    @Test
    void laPaginaAvisaDeLasPersonasNoAnalizadas() throws Exception {
        Sesion celador = celador();
        Usuario ana = crearUsuario("ana@sena.edu.co", "Aprendiz", Rol.USUARIO, "111111", u -> {
            u.setNombre("Ana Rodriguez");
            u.setFicha(FICHA);
        });
        Usuario luis = crearUsuario("luis@sena.edu.co", "Aprendiz", Rol.USUARIO, "222222", u -> {
            u.setNombre("Luis Perez");
            u.setFicha(FICHA);
        });
        llenarHastaElTope(ana, HOY);
        jornada(luis, HOY);

        JsonNode cuerpo = consultar(celador, "ficha", FICHA);
        assertThat(textos(cuerpo.get("omitidas"))).contains("Luis Perez");
    }

    @Test
    void rangoDeFechasInvertidoNoRompeLaVista() throws Exception {
        Sesion celador = celador();
        JsonNode cuerpo = consultar(celador, "usuarioId", id(celador.usuario()),
                "desde", "2026-08-30", "hasta", "2026-08-01");
        assertThat(cuerpo.get("fechaInicio").asText()).isEqualTo("2026-08-01");
        assertThat(cuerpo.get("fechaFin").asText()).isEqualTo("2026-08-30");
    }

    // --- Ayudas ---

    private Sesion celador() throws Exception {
        if (celador == null) {
            celador = entrarComo(Perfil.CELADOR);
        }
        return celador;
    }

    private Usuario ana() {
        return crearUsuario("ana@sena.edu.co", "123456");
    }

    private JsonNode consultar(Sesion sesion, String... parametros) throws Exception {
        MockHttpServletRequestBuilder peticion = get(RUTA);
        for (int i = 0; i < parametros.length; i += 2) {
            peticion.param(parametros[i], parametros[i + 1]);
        }
        return leer(mvc.perform(con(sesion, peticion)).andExpect(status().isOk()).andReturn());
    }

    private JsonNode bloque(Usuario persona, LocalDate desde, LocalDate hasta) throws Exception {
        return bloque(persona, desde, hasta, true);
    }

    private JsonNode bloque(Usuario persona, LocalDate desde, LocalDate hasta, boolean soloHabiles) throws Exception {
        return consultar(celador(), "usuarioId", id(persona), "desde", desde.toString(), "hasta", hasta.toString(),
                "soloHabiles", String.valueOf(soloHabiles)).get("personas").get(0);
    }

    private void acceso(Usuario persona, String tipo, LocalDateTime momento) {
        acceso(persona, tipo, momento, "Usuario", null, null);
    }

    private void acceso(Usuario persona, String tipo, LocalDateTime momento, String tipoReferencia, String equipos,
                        Long operadorId) {
        jdbc.update("INSERT INTO accesos (punto_id, referencia_id, tipo_referencia, tipo, fecha, equipos_ids, operador_id) "
                + "VALUES (1, ?, ?, ?, ?, ?, ?)", persona.getId(), tipoReferencia, tipo, Timestamp.valueOf(momento),
                equipos, operadorId);
    }

    private void jornada(Usuario persona, LocalDate dia) {
        jornada(persona, dia, 7, 13);
    }

    private void jornada(Usuario persona, LocalDate dia, int horaEntrada, int horaSalida) {
        acceso(persona, "Entrada", dia.atTime(horaEntrada, 0));
        acceso(persona, "Salida", dia.atTime(horaSalida, 0));
    }

    // El primer acceso hace de fecha de vinculación; este ancla evita que el recorte cambie el periodo
    private void anclarVinculacion(Usuario persona) {
        jornada(persona, LocalDate.of(2026, 7, 1));
    }

    private void llenarHastaElTope(Usuario persona, LocalDate dia) {
        jdbc.update("INSERT INTO accesos (punto_id, referencia_id, tipo_referencia, tipo, fecha) "
                + "SELECT 1, ?, 'Usuario', 'Entrada', CAST(? AS timestamp) + g * INTERVAL '1 second' "
                + "FROM generate_series(1, ?) g", persona.getId(), Timestamp.valueOf(dia.atTime(7, 0)),
                maximo("MAXIMO_ACCESOS"));
    }

    private long equipo(Usuario dueno, String nombre) {
        return jdbc.queryForObject("INSERT INTO equipos (nombre, tipo, usuario_id) VALUES (?, 'Portátil', ?) RETURNING id",
                Long.class, nombre, dueno.getId());
    }

    // Equivale a llamar dias_esperados: sin accesos, todos los días esperados salen como faltas
    private static int maximo(String constante) {
        return ((Number) ReflectionTestUtils.getField(HistorialService.class, constante)).intValue();
    }

    private static long diasDelRango(JsonNode cuerpo) {
        return ChronoUnit.DAYS.between(LocalDate.parse(cuerpo.get("fechaInicio").asText()),
                LocalDate.parse(cuerpo.get("fechaFin").asText())) + 1;
    }

    private static String id(Usuario usuario) {
        return String.valueOf(usuario.getId());
    }

    private static LocalDateTime momento(JsonNode valor) {
        return LocalDateTime.parse(valor.asText());
    }

    private static List<String> textos(JsonNode arreglo) {
        List<String> valores = new ArrayList<>();
        arreglo.forEach(v -> valores.add(v.asText()));
        return valores;
    }

}
