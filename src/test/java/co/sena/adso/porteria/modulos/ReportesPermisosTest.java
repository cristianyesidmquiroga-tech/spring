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
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

// Portería 2: tests/modulos/test_reportes_permisos.py
class ReportesPermisosTest extends PruebaIntegracion {

    @Autowired
    private Clock reloj;

    private Usuario aprendiz(String correo, String documento, String nombre) {
        return crearUsuario(correo, "Aprendiz", Rol.USUARIO, documento, u -> u.setNombre(nombre));
    }

    private void registrarEntrada(Sesion celador, Usuario persona) throws Exception {
        mvc.perform(conJson(celador, post("/api/porteria/movimientos"),
                        Map.of("tipoEntidad", "Usuario", "entidadId", persona.getId(), "tipo", "Entrada")))
                .andExpect(status().isCreated());
    }

    // Spring limita la exportación a un año, por eso el rango cubre alrededor de hoy y no 2020-2035
    private String exportarHoy(Sesion sesion) throws Exception {
        LocalDate hoy = LocalDate.now(reloj);
        MvcResult r = mvc.perform(con(sesion, get("/api/porteria/panel/exportar")
                        .param("desde", hoy.minusDays(1).toString())
                        .param("hasta", hoy.plusDays(1).toString())))
                .andExpect(status().isOk())
                .andReturn();
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private List<String> filaDonde(String csv, Predicate<List<String>> condicion) {
        return csv.replace("﻿", "").lines().skip(1)
                .map(linea -> Arrays.asList(linea.split(";", -1)))
                .filter(condicion)
                .findFirst().orElse(null);
    }

    @Test
    void aprendizNoEntraAAnalytics() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        mvc.perform(con(aprendiz, get("/api/porteria/panel/reportes/Aprendiz"))).andExpect(status().isForbidden());
    }

    @Test
    void celadorEntraAAnalytics() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        mvc.perform(con(celador, get("/api/porteria/panel/reportes/Aprendiz"))).andExpect(status().isOk());
    }

    @Test
    void adminEntraAAnalytics() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        mvc.perform(con(admin, get("/api/porteria/panel/reportes/Instructor"))).andExpect(status().isOk());
    }

    @Test
    void noAutenticadoNoEntraAAnalytics() throws Exception {
        mvc.perform(get("/api/porteria/panel/reportes/Aprendiz")).andExpect(status().isUnauthorized());
    }

    @Test
    void aprendizNoExporta() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        mvc.perform(con(aprendiz, get("/api/porteria/panel/exportar"))).andExpect(status().isForbidden());
    }

    @Test
    void celadorExporta() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        registrarEntrada(celador, aprendiz("aprendiz@sena.edu.co", "123123", "Juan Perez"));

        LocalDate hoy = LocalDate.now(reloj);
        MvcResult r = mvc.perform(con(celador, get("/api/porteria/panel/exportar")
                        .param("desde", hoy.minusDays(1).toString())
                        .param("hasta", hoy.plusDays(1).toString())))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(r.getResponse().getContentType()).startsWith("text/csv");
        assertThat(r.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("Juan Perez");
    }

    @Test
    void sinRangoDeFechasNoExporta() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        mvc.perform(con(celador, get("/api/porteria/panel/exportar")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    void rangoInvertidoNoExporta() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        mvc.perform(con(celador, get("/api/porteria/panel/exportar")
                        .param("desde", "2026-01-31").param("hasta", "2026-01-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    void exportarDejaConstanciaEnAuditoria() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        registrarEntrada(celador, aprendiz("aprendiz@sena.edu.co", "123123", "Juan Perez"));

        exportarHoy(celador);

        List<Long> autores = jdbc.queryForList("SELECT usuario_id FROM auditoria WHERE accion = ?", Long.class,
                "Exportación de histórico de accesos");
        assertThat(autores).containsExactly(celador.usuario().getId());
    }

    @Test
    void nombreConFormulaSeNeutraliza() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        registrarEntrada(celador, crearUsuario("atacante@sena.edu.co", "Aprendiz", Rol.USUARIO, "666666",
                u -> u.setNombre("=cmd|'/c calc'!A1")));

        List<String> fila = filaDonde(exportarHoy(celador), f -> f.get(0).equals("666666"));
        assertThat(fila).isNotNull();
        assertThat(fila.get(1)).as("la celda de nombre debía neutralizar la fórmula").startsWith("'");
        assertThat(fila.get(1)).doesNotStartWith("=");
    }

    @Test
    void documentoConFormulaSeNeutraliza() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        registrarEntrada(celador, aprendiz("atacante2@sena.edu.co", "+666666", "Ataque por documento"));

        List<String> fila = filaDonde(exportarHoy(celador), f -> f.size() > 1 && f.get(1).contains("Ataque por documento"));
        assertThat(fila).isNotNull();
        assertThat(fila.get(0)).startsWith("'");
    }

    @Test
    void nombreNormalNoSeToca() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        registrarEntrada(celador, aprendiz("normal@sena.edu.co", "222222", "Ana Torres"));

        List<String> fila = filaDonde(exportarHoy(celador), f -> f.get(0).equals("222222"));
        assertThat(fila).isNotNull();
        assertThat(fila.get(1)).isEqualTo("Ana Torres");
    }
}
