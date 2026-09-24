package co.sena.adso.porteria.roles.celador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import co.sena.adso.porteria.soporte.Perfil;
import java.util.Map;

// Portería 2: tests/roles/celador/test_perfil.py
class PerfilTest extends PruebaRol {

    @CeladorYPorteria
    void entraASuPerfil(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/perfil")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(sesion.usuario().getNombre()));
    }

    @CeladorYPorteria
    void actualizaSuTipoDeSangre(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(conJson(sesion, put("/api/perfil"), Map.of("tipoSangre", "O+")));
        assertThat(tipoSangre(sesion)).isEqualTo("O+");
    }

    @CeladorYPorteria
    void tipoDeSangreInvalidoSeRechaza(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(conJson(sesion, put("/api/perfil"), Map.of("tipoSangre", "X+")));
        assertThat(tipoSangre(sesion)).isNotEqualTo("X+");
    }

    private String tipoSangre(Sesion sesion) {
        return usuarioRepository.findById(sesion.usuario().getId()).orElseThrow().getTipoSangre();
    }
}
