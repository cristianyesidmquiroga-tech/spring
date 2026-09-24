package co.sena.adso.porteria.vistas.pases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/pases/test_pases.py
class PasesTest extends PruebaIntegracion {

    private static final Set<Perfil> ENTRAN = EnumSet.of(Perfil.ADMIN, Perfil.ADMINISTRADOR, Perfil.CELADOR, Perfil.PORTERIA);

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void accesoSegunElPerfil(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/porteria/pases")))
                .andExpect(ENTRAN.contains(perfil) ? status().isOk() : status().isForbidden());
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(get("/api/porteria/pases")).andExpect(status().isUnauthorized());
    }

    @Test
    void visitanteSinDocumentoNoSeCrea() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        mvc.perform(conJson(celador, post("/api/porteria/pases/visitantes"), Map.of("nombre", "Sin Doc")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM visitantes", Integer.class)).isZero();
    }

    @Test
    void creaYDesactivaUnObjeto() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        mvc.perform(conJson(celador, post("/api/porteria/pases/objetos"), Map.of("descripcion", "Taladro", "serial", "TAL-1")));
        Long id = jdbc.queryForObject("SELECT id FROM objetos_externos WHERE serial = 'TAL-1'", Long.class);
        mvc.perform(con(celador, post("/api/porteria/pases/objetos/" + id + "/desactivar")));
        assertThat(jdbc.queryForObject("SELECT activo FROM objetos_externos WHERE serial = 'TAL-1'", Boolean.class)).isFalse();
    }

    // El permiso se revisa antes que el cuerpo: aunque los datos no pasen la validación, responde 403
    @Test
    void crearSinPermisoDevuelve403() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        mvc.perform(conJson(aprendiz, post("/api/porteria/pases/visitantes"), Map.of("nombre", "X", "documento", "1")))
                .andExpect(status().isForbidden());
    }
}
