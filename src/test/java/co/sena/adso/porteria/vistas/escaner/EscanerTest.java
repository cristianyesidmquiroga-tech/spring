package co.sena.adso.porteria.vistas.escaner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

// Portería 2: tests/vistas/escaner/test_escaner.py
class EscanerTest extends PruebaIntegracion {

    private static final Set<Perfil> ENTRAN = EnumSet.of(Perfil.ADMIN, Perfil.ADMINISTRADOR, Perfil.CELADOR, Perfil.PORTERIA);

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void accesoSegunElPerfil(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/porteria/verificar").param("codigo", "999")))
                .andExpect(ENTRAN.contains(perfil) ? status().isOk() : status().isForbidden());
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(get("/api/porteria/verificar").param("codigo", "999")).andExpect(status().isUnauthorized());
    }

    @Test
    void documentoInexistenteNoSeEncuentra() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        mvc.perform(con(celador, get("/api/porteria/verificar").param("codigo", "999")))
                .andExpect(jsonPath("$.encontrado").value(false));
    }

    @Test
    void verificarSinPermisoDevuelve403() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        mvc.perform(con(aprendiz, get("/api/porteria/verificar").param("codigo", "123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void registraEntradaYSalida() throws Exception {
        Usuario otro = crearUsuario("otra.persona@sena.edu.co", "3000000002");
        Sesion porteria = entrarComo(Perfil.PORTERIA);
        for (String tipo : new String[] {"Entrada", "Salida"}) {
            mvc.perform(conJson(porteria, post("/api/porteria/movimientos"),
                            Map.of("tipoEntidad", "Usuario", "entidadId", otro.getId(), "tipo", tipo)))
                    .andExpect(status().isCreated());
        }
        assertThat(jdbc.queryForList("SELECT tipo FROM accesos WHERE referencia_id = ? ORDER BY id", String.class, otro.getId()))
                .containsExactly("Entrada", "Salida");
    }

    @Test
    void registraUnIncidente() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        mvc.perform(conJson(celador, post("/api/porteria/incidentes"),
                        Map.of("tipoEntidad", "Usuario", "detalles", "Equipo sin registrar")))
                .andExpect(status().isCreated());
    }
}
