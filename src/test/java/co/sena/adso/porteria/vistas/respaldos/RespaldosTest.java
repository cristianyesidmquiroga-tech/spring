package co.sena.adso.porteria.vistas.respaldos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/respaldos/test_respaldos.py
class RespaldosTest extends PruebaIntegracion {

    private static final String RUTA = "/api/admin/respaldos";

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
    void descargarSinPermisoNoDescarga() throws Exception {
        mvc.perform(con(entrarComo(Perfil.ADMINISTRADOR), get(RUTA + "/respaldo.xlsx"))).andExpect(status().isForbidden());
    }

    // El cortafuegos de Spring rechaza el %2F con 400 antes de llegar; un nombre fuera del patrón da 404
    @Test
    void noDescargaFueraDeLaCarpeta() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        mvc.perform(con(admin, get(RUTA + "/..%2F..%2Fconfig.py"))).andExpect(status().is4xxClientError());
        mvc.perform(con(admin, get(RUTA + "/..config.py"))).andExpect(status().isNotFound());
    }
}
