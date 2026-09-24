package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Rol;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.repository.AccesoRepository;
import co.sena.adso.porteria.repository.EquipoRepository;
import co.sena.adso.porteria.repository.UsuarioRepository;
import co.sena.adso.porteria.service.AuthService;
import co.sena.adso.porteria.service.HistorialService;
import co.sena.adso.porteria.service.HistorialService.Filtros;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// Portería 2: tests/modulos/test_historial_persona.py
class HistorialAsistenciaTest extends PruebaIntegracion {

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

    // La tabla fichas no se limpia entre pruebas
    @AfterEach
    void quitarFichaDePrueba() {
        jdbc.update("UPDATE usuarios SET ficha_id = NULL WHERE ficha_id IN (SELECT id FROM fichas WHERE numero = ?)", FICHA);
        jdbc.update("DELETE FROM fichas WHERE numero = ?", FICHA);
    }

    // --- Días asistidos y faltados ---

    @Test
    void diasEsperadosSoloCuentaHabilesYNoElFuturo() {
        // Lunes 24 a domingo 30 de agosto; hoy es el jueves 27
        List<LocalDate> esperados = esperadosSinAccesos(LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 30),
                true, LocalDate.of(2026, 8, 27));
        assertThat(esperados).containsExactly(LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 25),
                LocalDate.of(2026, 8, 26), LocalDate.of(2026, 8, 27));
    }

    @Test
    void diasEsperadosPuedeIncluirFinDeSemana() {
        List<LocalDate> esperados = esperadosSinAccesos(LocalDate.of(2026, 8, 29), LocalDate.of(2026, 8, 30),
                false, LocalDate.of(2026, 8, 31));
        assertThat(esperados).containsExactly(LocalDate.of(2026, 8, 29), LocalDate.of(2026, 8, 30));
    }

    @Test
    void asistenciasYFaltasDelPeriodo() throws Exception {
        Usuario persona = ana();
        LocalDate inicio = LocalDate.of(2026, 8, 3);
        LocalDate fin = LocalDate.of(2026, 8, 9);
        for (int desplazamiento : new int[] {0, 2}) {
            jornada(persona, inicio.plusDays(desplazamiento));
        }

        JsonNode resumen = bloque(persona, inicio, fin, false).get("resumen");
        assertThat(resumen.get("diasEsperados").asInt()).isEqualTo(7);
        assertThat(resumen.get("diasAsistidos").asInt()).isEqualTo(2);
        assertThat(resumen.get("diasFaltados").asInt()).isEqualTo(5);
        assertThat(resumen.get("porcentajeAsistencia").asDouble()).isEqualTo(28.6);
        int faltas = 0;
        for (JsonNode valor : resumen.get("faltasPorDia")) {
            faltas += valor.asInt();
        }
        assertThat(faltas).isEqualTo(5);
        assertThat(resumen.get("promedioPermanenciaMinutos").asLong()).isEqualTo(360);
    }

    @Test
    void variasEntradasElMismoDiaCuentanUnSoloDia() throws Exception {
        Usuario persona = ana();
        for (int[] horas : new int[][] {{7, 9}, {10, 12}, {14, 16}}) {
            acceso(persona, "Entrada", HOY.atTime(horas[0], 0));
            acceso(persona, "Salida", HOY.atTime(horas[1], 0));
        }

        JsonNode resumen = bloque(persona, HOY, HOY, false).get("resumen");
        assertThat(resumen.get("totalMovimientos").asInt()).isEqualTo(3);
        assertThat(resumen.get("diasAsistidos").asInt()).isEqualTo(1);
        assertThat(resumen.get("diasFaltados").asInt()).isZero();
    }

    @Test
    void periodoFueraDeRangoNoTraeMovimientos() throws Exception {
        Usuario persona = ana();
        acceso(persona, "Entrada", HOY.minusDays(90).atTime(7, 0));

        JsonNode bloque = bloque(persona, HOY.minusDays(5), HOY);
        assertThat(bloque.get("movimientos")).isEmpty();
        assertThat(bloque.get("resumen").get("diasAsistidos").asInt()).isZero();
    }

    // --- Ventana exigible: nadie falta antes de estar vinculado ni después de terminar su ficha ---

    @Test
    void aprendizRecienVinculadoConAsistenciaPerfecta() throws Exception {
        Usuario persona = crearUsuario("nuevo@sena.edu.co", "123456");
        for (LocalDate dia = LocalDate.of(2026, 8, 3); !dia.isAfter(LocalDate.of(2026, 8, 28)); dia = dia.plusDays(1)) {
            if (habil(dia)) {
                jornada(persona, dia, 7, 17);
            }
        }

        JsonNode resumen = bloque(persona, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 8, 28)).get("resumen");
        assertThat(resumen.get("diasAsistidos").asInt()).isEqualTo(20);
        assertThat(resumen.get("diasEsperados").asInt()).isEqualTo(20);
        assertThat(resumen.get("diasFaltados").asInt()).isZero();
        assertThat(resumen.get("porcentajeAsistencia").asDouble()).isEqualTo(100.0);
        assertThat(resumen.get("periodoEvaluadoInicio").asText()).isEqualTo("2026-08-03");
        assertThat(textos(resumen.get("motivosVentana"))).contains("vinculacion");
    }

    @Test
    void egresadoNoAcumulaFaltasTrasTerminarSuFicha() throws Exception {
        Long fichaId = jdbc.queryForObject("INSERT INTO fichas (numero, programa, fecha_finalizacion) VALUES (?, 'ADSO', ?) "
                + "RETURNING id", Long.class, FICHA, java.sql.Date.valueOf(LocalDate.of(2026, 8, 14)));
        Usuario persona = crearUsuario("egresado@sena.edu.co", "Aprendiz", Rol.USUARIO, "123456", u -> u.setFicha(FICHA));
        jdbc.update("UPDATE usuarios SET ficha_id = ? WHERE id = ?", fichaId, persona.getId());
        for (LocalDate dia = LocalDate.of(2026, 8, 3); !dia.isAfter(LocalDate.of(2026, 8, 14)); dia = dia.plusDays(1)) {
            if (habil(dia)) {
                jornada(persona, dia);
            }
        }

        JsonNode resumen = bloque(persona, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 31)).get("resumen");
        assertThat(resumen.get("periodoEvaluadoFin").asText()).isEqualTo("2026-08-14");
        assertThat(resumen.get("diasEsperados").asInt()).isEqualTo(10);
        assertThat(resumen.get("diasFaltados").asInt()).isZero();
        assertThat(textos(resumen.get("motivosVentana"))).contains("fin_de_ficha");
    }

    @Test
    void sinNingunIngresoSeAvisaQueNoHayReferencia() throws Exception {
        Usuario persona = crearUsuario("fantasma@sena.edu.co", "123456");
        JsonNode resumen = bloque(persona, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 7)).get("resumen");
        assertThat(textos(resumen.get("motivosVentana"))).contains("sin_referencia_de_vinculacion");
        assertThat(resumen.get("diasFaltados").asInt()).isEqualTo(5);
    }

    @Test
    void periodoAnteriorALaVinculacionNoDaPorcentaje() throws Exception {
        Usuario persona = crearUsuario("nuevo@sena.edu.co", "123456");
        jornada(persona, LocalDate.of(2026, 8, 3));

        JsonNode resumen = bloque(persona, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 9)).get("resumen");
        assertThat(resumen.get("diasEsperados").asInt()).isZero();
        assertThat(resumen.get("diasFaltados").asInt()).isZero();
        assertThat(resumen.get("porcentajeAsistencia").isNull()).isTrue();
    }

    // --- Turno nocturno: un día con presencia comprobada no puede figurar como falta ---

    @Test
    void laSalidaDeMadrugadaMarcaAsistidoEseDia() throws Exception {
        Usuario celadorNocturno = nocturno();
        // Entra el domingo a las 22:00 y sale el lunes a las 06:00
        acceso(celadorNocturno, "Entrada", LocalDateTime.of(2026, 8, 2, 22, 0));
        acceso(celadorNocturno, "Salida", LocalDateTime.of(2026, 8, 3, 6, 0));

        LocalDate lunes = LocalDate.of(2026, 8, 3);
        JsonNode bloque = bloque(celadorNocturno, lunes, lunes);
        JsonNode movimiento = bloque.get("movimientos").get(0);
        JsonNode resumen = bloque.get("resumen");
        // El DTO no publica los días del movimiento: se ven en el cómputo (lunes asistido, domingo fuera)
        assertThat(momento(movimiento.get("entrada")).toLocalDate()).isEqualTo(LocalDate.of(2026, 8, 2));
        assertThat(momento(movimiento.get("salida")).toLocalDate()).isEqualTo(lunes);
        assertThat(resumen.get("diasFueraDeComputo").asInt()).isEqualTo(1);
        assertThat(resumen.get("periodoEvaluadoInicio").asText()).isEqualTo(lunes.toString());
        assertThat(resumen.get("diasAsistidos").asInt()).isEqualTo(1);
        assertThat(resumen.get("diasFaltados").asInt()).isZero();
    }

    @Test
    void celadorNocturnoNoAcumulaUnaFaltaPorNoche() throws Exception {
        Usuario celadorNocturno = nocturno();
        // Tres turnos domingo 22:00 a lunes 06:00
        for (LocalDate domingo : List.of(LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 9), LocalDate.of(2026, 8, 16))) {
            acceso(celadorNocturno, "Entrada", domingo.atTime(22, 0));
            acceso(celadorNocturno, "Salida", domingo.plusDays(1).atTime(6, 0));
        }

        JsonNode resumen = bloque(celadorNocturno, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 21)).get("resumen");
        List<LocalDate> lunes = List.of(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 17));
        // Los únicos 3 días asistidos del cómputo son los lunes: ninguno quedó como falta
        assertThat(resumen.get("diasAsistidos").asInt()).isEqualTo(3);
        assertThat(fechas(resumen.get("fechasFaltadas"))).doesNotContainAnyElementsOf(lunes);
        assertThat(resumen.get("faltasPorDia").path("Lunes").asInt(0)).isZero();
        // Las noches del domingo se ven, pero no entran al cálculo hábil
        assertThat(resumen.get("diasFueraDeComputo").asInt()).isEqualTo(3);
    }

    // --- Porcentaje de asistencia: numerador y denominador sobre el mismo calendario ---

    @Test
    void asistirLosSieteDiasNoPasaDeCien() throws Exception {
        Usuario persona = ana();
        for (int desplazamiento = 0; desplazamiento < 7; desplazamiento++) {
            jornada(persona, LocalDate.of(2026, 8, 3).plusDays(desplazamiento));
        }

        JsonNode resumen = bloque(persona, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 9), true).get("resumen");
        assertThat(resumen.get("diasEsperados").asInt()).isEqualTo(5);
        assertThat(resumen.get("diasAsistidos").asInt()).isEqualTo(5);
        assertThat(resumen.get("porcentajeAsistencia").asDouble()).isEqualTo(100.0);
        assertThat(resumen.get("diasFueraDeComputo").asInt()).isEqualTo(2);
    }

    @Test
    void asistirSoloEnFinDeSemanaNoInventaAsistencia() throws Exception {
        Usuario persona = ana();
        anclarVinculacion(persona);
        jornada(persona, LocalDate.of(2026, 8, 8));
        jornada(persona, LocalDate.of(2026, 8, 9));

        JsonNode bloque = bloque(persona, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 9), true);
        JsonNode resumen = bloque.get("resumen");
        assertThat(resumen.get("diasEsperados").asInt()).isEqualTo(5);
        assertThat(resumen.get("diasAsistidos").asInt()).isZero();
        assertThat(resumen.get("diasFaltados").asInt()).isEqualTo(5);
        assertThat(resumen.get("porcentajeAsistencia").asDouble()).isEqualTo(0.0);
        // La asistencia real no se oculta: sábado y domingo quedan fuera del cómputo
        assertThat(resumen.get("diasFueraDeComputo").asInt()).isEqualTo(2);
        assertThat(bloque.get("movimientos").findValuesAsText("fecha")).containsExactly("2026-08-08", "2026-08-09");
    }

    @Test
    void contandoTodosLosDiasElFinDeSemanaSiSuma() throws Exception {
        Usuario persona = ana();
        for (int desplazamiento = 0; desplazamiento < 7; desplazamiento++) {
            jornada(persona, LocalDate.of(2026, 8, 3).plusDays(desplazamiento));
        }

        JsonNode resumen = bloque(persona, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 9), false).get("resumen");
        assertThat(resumen.get("diasEsperados").asInt()).isEqualTo(7);
        assertThat(resumen.get("porcentajeAsistencia").asDouble()).isEqualTo(100.0);
        assertThat(resumen.get("diasFueraDeComputo").asInt()).isZero();
    }

    // --- Día más faltado: conducta, no cuántos lunes trae el calendario ---

    @Test
    void usaLaTasaYNoElConteoBruto() throws Exception {
        Usuario persona = ana();
        anclarVinculacion(persona);
        LocalDate ultimoLunes = LocalDate.of(2026, 8, 24);
        for (LocalDate dia = LocalDate.of(2026, 8, 3); !dia.isAfter(ultimoLunes); dia = dia.plusDays(1)) {
            // Asiste martes, jueves y viernes; un solo lunes; ningún miércoles
            if (List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY).contains(dia.getDayOfWeek())
                    || dia.equals(ultimoLunes)) {
                jornada(persona, dia);
            }
        }

        JsonNode resumen = bloque(persona, LocalDate.of(2026, 8, 3), ultimoLunes).get("resumen");
        assertThat(resumen.get("faltasPorDia").get("Lunes").asInt()).isEqualTo(3);
        assertThat(resumen.get("faltasPorDia").get("Miércoles").asInt()).isEqualTo(3);
        assertThat(resumen.get("detalleDiasSemana").get("Lunes").get("oportunidades").asInt()).isEqualTo(4);
        assertThat(resumen.get("detalleDiasSemana").get("Miércoles").get("oportunidades").asInt()).isEqualTo(3);
        assertThat(resumen.get("diaMasFaltado").asText()).isEqualTo("Miércoles");
    }

    @Test
    void quienNuncaAsisteNoTieneUnDiaPeor() throws Exception {
        Usuario persona = ana();
        anclarVinculacion(persona);

        JsonNode resumen = bloque(persona, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 21)).get("resumen");
        assertThat(resumen.get("diasFaltados").asInt()).isEqualTo(15);
        assertThat(resumen.get("diaMasFaltado").isNull()).isTrue();
        assertThat(resumen.get("motivoSinDia").asText()).isEqualTo("empate");
        assertThat(resumen.get("diasEmpatados")).hasSize(5);
    }

    @Test
    void empateEntreDosDiasNoDevuelveElPrimero() throws Exception {
        Usuario persona = ana();
        anclarVinculacion(persona);
        for (LocalDate dia = LocalDate.of(2026, 8, 3); !dia.isAfter(LocalDate.of(2026, 8, 21)); dia = dia.plusDays(1)) {
            // Falta todos los lunes y miércoles, asiste el resto
            if (List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY).contains(dia.getDayOfWeek())) {
                jornada(persona, dia);
            }
        }

        JsonNode resumen = bloque(persona, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 21)).get("resumen");
        assertThat(resumen.get("diaMasFaltado").isNull()).isTrue();
        assertThat(textos(resumen.get("diasEmpatados"))).containsExactly("Lunes", "Miércoles");
    }

    @Test
    void periodoCortoNoAfirmaUnPatron() throws Exception {
        Usuario persona = ana();
        anclarVinculacion(persona);

        JsonNode resumen = bloque(persona, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 7)).get("resumen");
        assertThat(resumen.get("diasFaltados").asInt()).isEqualTo(5);
        assertThat(resumen.get("diaMasFaltado").isNull()).isTrue();
        assertThat(resumen.get("motivoSinDia").asText()).isEqualTo("datos_insuficientes");
    }

    @Test
    void sinFaltasNoHayDiaPeor() throws Exception {
        Usuario persona = ana();
        for (LocalDate dia = LocalDate.of(2026, 8, 3); !dia.isAfter(LocalDate.of(2026, 8, 21)); dia = dia.plusDays(1)) {
            if (habil(dia)) {
                jornada(persona, dia);
            }
        }

        JsonNode resumen = bloque(persona, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 21)).get("resumen");
        assertThat(resumen.get("diaMasFaltado").isNull()).isTrue();
        assertThat(resumen.get("motivoSinDia").asText()).isEqualTo("sin_faltas");
    }

    // --- Cortes del rango: el filtro de fechas no puede fabricar anomalías ---

    @Test
    void entradaElUltimoDiaConservaSuSalida() throws Exception {
        Usuario celadorNocturno = nocturno();
        acceso(celadorNocturno, "Entrada", LocalDateTime.of(2026, 8, 7, 22, 0));
        acceso(celadorNocturno, "Salida", LocalDateTime.of(2026, 8, 8, 6, 0));

        JsonNode bloque = bloque(celadorNocturno, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 7));
        JsonNode movimiento = bloque.get("movimientos").get(0);
        assertThat(movimiento.get("salida").isNull()).isFalse();
        assertThat(movimiento.get("permanenciaMinutos").asLong()).isEqualTo(480);
        assertThat(movimiento.get("salidaPosteriorAlRango").asBoolean()).isTrue();
        assertThat(bloque.get("resumen").get("sinSalida").asInt()).isZero();
    }

    @Test
    void salidaElPrimerDiaConservaSuEntrada() throws Exception {
        Usuario celadorNocturno = nocturno();
        acceso(celadorNocturno, "Entrada", LocalDateTime.of(2026, 8, 2, 22, 0));
        acceso(celadorNocturno, "Salida", LocalDateTime.of(2026, 8, 3, 6, 0));

        JsonNode movimiento = bloque(celadorNocturno, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 7))
                .get("movimientos").get(0);
        assertThat(movimiento.get("entrada").isNull()).isFalse();
        assertThat(movimiento.get("entradaFueraDeVentana").asBoolean()).isFalse();
        assertThat(movimiento.get("entradaPreviaAlRango").asBoolean()).isTrue();
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

    private Usuario nocturno() {
        return crearUsuario("noct@sena.edu.co", "Celador", Rol.USUARIO, "777001", u -> { });
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

    private List<LocalDate> esperadosSinAccesos(LocalDate desde, LocalDate hasta, boolean soloHabiles, LocalDate hoy) {
        AuthService auth = mock(AuthService.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        Usuario solicitante = mock(Usuario.class);
        Usuario persona = mock(Usuario.class);
        when(solicitante.puedeOperarPorteria()).thenReturn(true);
        when(auth.usuarioActual()).thenReturn(solicitante);
        when(persona.getId()).thenReturn(1L);
        when(usuarios.buscarParaHistorial(anyBoolean(), anyList(), anyString(), anyString(), anyString(), any()))
                .thenReturn(List.of(persona));
        HistorialService servicio = new HistorialService(mock(AccesoRepository.class), usuarios,
                mock(EquipoRepository.class), auth, Clock.fixed(hoy.atTime(10, 0).atZone(BOGOTA).toInstant(), BOGOTA));

        var resumen = servicio.consultar(new Filtros(List.of(1L), null, null, null, desde, hasta, soloHabiles))
                .personas().get(0).resumen();
        assertThat(resumen.diasEsperados()).isEqualTo(resumen.fechasFaltadas().size());
        return resumen.fechasFaltadas();
    }

    private static boolean habil(LocalDate dia) {
        return dia.getDayOfWeek().getValue() <= DayOfWeek.FRIDAY.getValue();
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

    private static List<LocalDate> fechas(JsonNode arreglo) {
        return textos(arreglo).stream().map(LocalDate::parse).toList();
    }
}
