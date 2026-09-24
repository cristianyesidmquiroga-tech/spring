package co.sena.adso.porteria.vistas.fichas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/fichas/test_fichas.py
class FichasTest extends PruebaIntegracion {

    private static final String RUTA = "/api/admin/fichas";

    private long contar(String numero) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM fichas WHERE numero = ?", Long.class, numero);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void accesoSegunElPerfil(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get(RUTA)))
                .andExpect(perfil == Perfil.ADMIN ? status().isOk() : status().isForbidden());
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    }

    @Test
    void creaEditaYArchiva() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        mvc.perform(conJson(admin, post(RUTA), Map.of("numero", "2758291", "programa", "Sistemas")));
        Long id = jdbc.queryForObject("SELECT id FROM fichas WHERE numero = '2758291'", Long.class);
        mvc.perform(conJson(admin, put(RUTA + "/{id}", id), Map.of("numero", "2758291", "programa", "Software")));
        mvc.perform(con(admin, patch(RUTA + "/{id}/archivar", id)));

        Map<String, Object> ficha = jdbc.queryForMap("SELECT programa, activa FROM fichas WHERE id = ?", id);
        assertThat(ficha.get("programa")).isEqualTo("Software");
        assertThat(ficha.get("activa")).isEqualTo(false);
    }

    @Test
    void numeroInvalidoNoSeCrea() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        mvc.perform(conJson(admin, post(RUTA), Map.of("numero", "ABC", "programa", "X")))
                .andExpect(status().isBadRequest());
        assertThat(contar("ABC")).isZero();
    }

    @Test
    void fichaRepetidaNoSeDuplica() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        for (int i = 0; i < 2; i++) {
            mvc.perform(conJson(admin, post(RUTA), Map.of("numero", "2758291", "programa", "Sistemas")));
        }
        assertThat(contar("2758291")).isEqualTo(1);
    }
}
