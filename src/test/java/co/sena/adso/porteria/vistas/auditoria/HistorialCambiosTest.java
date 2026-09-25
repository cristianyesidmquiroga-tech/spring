package co.sena.adso.porteria.vistas.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/historial_cambios/test_historial_cambios.py
class HistorialCambiosTest extends PruebaIntegracion {

    private static final String RUTA = "/api/admin/auditoria";

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void accesoSegunElPerfil(Perfil perfil) throws Exception {
        mvc.perform(con(entrarComo(perfil), get(RUTA)))
                .andExpect(perfil == Perfil.ADMIN ? status().isOk() : status().isForbidden());
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    }

    @Test
    void muestraLosCambiosRegistrados() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        jdbc.update("INSERT INTO auditoria (usuario_id, nombre_usuario, tabla_afectada, registro_id, accion) "
                + "VALUES (?, ?, 'usuarios', 1, 'Cambio de prueba')", admin.usuario().getId(), admin.usuario().getNombre());
        assertThat(mvc.perform(con(admin, get(RUTA))).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("Cambio de prueba");
    }
}
