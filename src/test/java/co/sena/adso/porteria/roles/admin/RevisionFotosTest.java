package co.sena.adso.porteria.roles.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.roles.PruebaRol;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// Portería 2: tests/roles/admin/test_revision_fotos.py
class RevisionFotosTest extends PruebaRol {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"pendiente", "aprobada", "rechazada", "todos"})
    void entraARevisionDeFotos(String estado) throws Exception {
        Sesion sesion = sesion();
        mvc.perform(con(sesion, get("/api/admin/fotos").param("estado", estado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(estado));
    }

    @Test
    void decisionInvalidaSeRechaza() throws Exception {
        Sesion sesion = sesion();
        Usuario otro = otroUsuario();
        mvc.perform(conJson(sesion, post("/api/admin/fotos/{id}/revision", otro.getId()), Map.of("aprobada", "otra")))
                .andExpect(status().isBadRequest());
    }
}
