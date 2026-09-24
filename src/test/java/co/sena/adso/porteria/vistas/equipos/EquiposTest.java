package co.sena.adso.porteria.vistas.equipos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.web.servlet.ResultActions;

// Portería 2: tests/vistas/equipos/test_equipos.py
class EquiposTest extends PruebaIntegracion {

    private static final Set<Perfil> NO_REGISTRAN = EnumSet.of(Perfil.CELADOR, Perfil.PORTERIA, Perfil.TRABAJADOR);

    private ResultActions registrar(Sesion sesion, Map<String, String> datos) throws Exception {
        return mvc.perform(conJson(sesion, post("/api/equipos"), datos));
    }

    private int equiposDe(Sesion sesion) {
        return jdbc.queryForObject("SELECT count(*) FROM equipos WHERE usuario_id = ?", Integer.class,
                sesion.usuario().getId());
    }

    // Flask respondía 400 a quien no registra equipos; la API usa 403
    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void registroSegunElPerfil(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        ResultActions r = registrar(sesion, Map.of("nombre", "Portatil", "tipo", "Portátil"));
        if (NO_REGISTRAN.contains(perfil)) {
            r.andExpect(status().isForbidden());
            assertThat(equiposDe(sesion)).isZero();
        } else {
            r.andExpect(status().isCreated());
            assertThat(equiposDe(sesion)).isEqualTo(1);
        }
    }

    // Flask respondía 400 al sexto equipo; la API usa 422
    @Test
    void maximoCincoEquipos() throws Exception {
        Sesion sesion = entrarComo(Perfil.APRENDIZ);
        for (int i = 0; i < 5; i++) {
            registrar(sesion, Map.of("nombre", "Equipo " + i)).andExpect(status().isCreated());
        }
        registrar(sesion, Map.of("nombre", "Equipo 5")).andExpect(status().isUnprocessableEntity());
        assertThat(equiposDe(sesion)).isEqualTo(5);
    }

    // Flask respondía 400; la API usa 422
    @Test
    void serialRepetidoSeRechaza() throws Exception {
        Sesion sesion = entrarComo(Perfil.APRENDIZ);
        registrar(sesion, Map.of("nombre", "A", "serial", "SN1"));
        registrar(sesion, Map.of("nombre", "B", "serial", "SN1")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void tipoInvalidoSeRechaza() throws Exception {
        Sesion sesion = entrarComo(Perfil.APRENDIZ);
        registrar(sesion, Map.of("nombre", "A", "tipo", "Nevera")).andExpect(status().isBadRequest());
    }

    // Flask respondía 400; la API usa 403
    @Test
    void noBorraElEquipoDeOtro() throws Exception {
        Usuario otro = crearUsuario("otra.persona@sena.edu.co", "3000000002");
        Long equipoId = jdbc.queryForObject("INSERT INTO equipos (nombre, tipo, usuario_id) VALUES ('Ajeno', 'Otro', ?) "
                + "RETURNING id", Long.class, otro.getId());
        Sesion sesion = entrarComo(Perfil.APRENDIZ);
        mvc.perform(con(sesion, delete("/api/equipos/" + equipoId))).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM equipos WHERE id = ?", Integer.class, equipoId)).isEqualTo(1);
    }
}
