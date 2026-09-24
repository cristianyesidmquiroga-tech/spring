package co.sena.adso.porteria.roles.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import java.util.Map;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/admin/test_perfil.py
class PerfilTest extends PruebaRol {

    @Test
    void entraASuPerfil() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(con(sesion, get("/api/perfil")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(sesion.usuario().getNombre()));
    }

    @Test
    void actualizaSuTipoDeSangre() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(conJson(sesion, put("/api/perfil"), Map.of("tipoSangre", "O+")));
        assertThat(tipoSangre(sesion)).isEqualTo("O+");
    }

    @Test
    void tipoDeSangreInvalidoSeRechaza() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(conJson(sesion, put("/api/perfil"), Map.of("tipoSangre", "X+")));
        assertThat(tipoSangre(sesion)).isNotEqualTo("X+");
    }

    private String tipoSangre(Sesion sesion) {
        return usuarioRepository.findById(sesion.usuario().getId()).orElseThrow().getTipoSangre();
    }
}
