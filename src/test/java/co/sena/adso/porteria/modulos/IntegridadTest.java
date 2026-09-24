package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

// Portería 2: tests/modulos/test_integridad.py
class IntegridadTest extends PruebaIntegracion {

    private static final String INSERTAR_OBJETO = "INSERT INTO objetos_externos (descripcion, serial, codigo, fecha_creacion) "
            + "VALUES (?, ?, ?, CURRENT_TIMESTAMP)";

    private Set<String> indices(String tabla) {
        return new HashSet<>(jdbc.queryForList("""
                SELECT string_agg(a.attname, ',' ORDER BY k.orden)
                FROM pg_index i
                JOIN pg_class t ON t.oid = i.indrelid
                CROSS JOIN LATERAL unnest(i.indkey) WITH ORDINALITY AS k(attnum, orden)
                JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = k.attnum
                WHERE t.relname = ?
                GROUP BY i.indexrelid""", String.class, tabla));
    }

    private Timestamp ahora() {
        return Timestamp.valueOf(LocalDateTime.now());
    }

    @Test
    void operadorDePorteria() throws Exception {
        Usuario celador = crearUsuario("celador@sena.edu.co", "Celador", "Usuario", "444", u -> { });
        Long accesoId = jdbc.queryForObject("INSERT INTO accesos (punto_id, referencia_id, tipo_referencia, tipo, fecha, "
                + "operador_id) VALUES (1, 12345, 'Visitante', 'Entrada', ?, ?) RETURNING id", Long.class, ahora(), celador.getId());
        Long auditoriaId = jdbc.queryForObject("INSERT INTO auditoria (usuario_id, nombre_usuario, tabla_afectada, "
                + "registro_id, accion, fecha) VALUES (?, ?, 'accesos', 1, 'Salida forzada', ?) RETURNING id",
                Long.class, celador.getId(), celador.getNombre(), ahora());

        Sesion admin = entrarComo(Perfil.ADMIN);
        mvc.perform(conJson(admin, delete("/api/admin/usuarios/{id}", celador.getId()),
                        Map.of("autorizadoPor", "Coordinacion", "motivo", "Prueba de integridad")))
                .andExpect(status().isNoContent());

        Map<String, Object> acceso = jdbc.queryForMap("SELECT operador_id FROM accesos WHERE id = ?", accesoId);
        assertThat(acceso.get("operador_id")).isNull();
        Map<String, Object> auditoria = jdbc.queryForMap(
                "SELECT usuario_id, nombre_usuario FROM auditoria WHERE id = ?", auditoriaId);
        assertThat(auditoria.get("usuario_id")).isNull();
        assertThat(auditoria.get("nombre_usuario")).isEqualTo(celador.getNombre());
    }

    // En Spring el paso del equipo queda en accesos.equipos_ids, sin clave foránea que impida borrarlo
    @Test
    void equipoQueCruzoLaPorteria() throws Exception {
        Usuario persona = crearUsuario("duenio@sena.edu.co", "555");
        Long equipoId = jdbc.queryForObject("INSERT INTO equipos (nombre, serial, tipo, usuario_id) "
                + "VALUES ('Portatil HP', 'SN-001', 'Portátil', ?) RETURNING id", Long.class, persona.getId());
        jdbc.update("INSERT INTO accesos (punto_id, referencia_id, tipo_referencia, tipo, fecha, equipos_ids) "
                + "VALUES (1, ?, 'Usuario', 'Entrada', ?, ?)", persona.getId(), ahora(), equipoId.toString());

        Sesion sesion = new Sesion(persona, iniciarSesion(persona.getCorreo(), CLAVE));
        mvc.perform(con(sesion, delete("/api/equipos/{id}", equipoId))).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM equipos WHERE id = ?", Integer.class, equipoId)).isZero();
    }

    @Test
    void indiceCompuestoDelEscaner() {
        assertThat(indices("accesos")).contains("referencia_id,tipo_referencia,fecha");
    }

    @Test
    void indicePorFechaDeAcceso() {
        assertThat(indices("accesos")).contains("fecha");
    }

    // carnet_id y movimientos_* no existen (Spring los unificó en accesos); asistencia_clases llega en la fase 3
    @Test
    void indicesDeClavesAjenas() {
        assertThat(indices("accesos")).contains("punto_id", "operador_id");
        assertThat(indices("auditoria")).contains("usuario_id");
        assertThat(indices("equipos")).contains("usuario_id");
    }

    @Test
    void serialDeObjetoExternoEsUnico() {
        jdbc.update(INSERTAR_OBJETO, "Taladro", "OBJ-1", "OBJ-A");
        assertThatThrownBy(() -> jdbc.update(INSERTAR_OBJETO, "Otro taladro", "OBJ-1", "OBJ-B"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
