package co.sena.adso.porteria.roles.administrador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.roles.PruebaRol;
import java.util.Map;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/administrador/test_escaner.py
class EscanerTest extends PruebaRol {

    @Test
    void entraAlEscaner() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(con(sesion, get("/api/porteria/verificar").param("codigo", "999"))).andExpect(status().isOk());
    }

    @Test
    void verificaUnDocumento() throws Exception {
        Sesion sesion = sesion();
        Usuario otro = otroUsuario();
        mvc.perform(con(sesion, get("/api/porteria/verificar").param("codigo", otro.getDocumento())))
                .andExpect(jsonPath("$.encontrado").value(true));
    }

    @Test
    void registraUnaEntrada() throws Exception {
        Sesion sesion = sesion();
        Usuario otro = otroUsuario();
        mvc.perform(conJson(sesion, post("/api/porteria/movimientos"),
                        Map.of("tipoEntidad", "Usuario", "entidadId", otro.getId(), "tipo", "Entrada")))
                .andExpect(status().isCreated());
    }
}
