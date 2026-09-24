package co.sena.adso.porteria.roles.celador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.roles.PruebaRol;
import co.sena.adso.porteria.soporte.Perfil;
import java.util.Map;

// Portería 2: tests/roles/celador/test_escaner.py
class EscanerTest extends PruebaRol {

    @CeladorYPorteria
    void entraAlEscaner(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/porteria/verificar").param("codigo", "999"))).andExpect(status().isOk());
    }

    @CeladorYPorteria
    void verificaUnDocumento(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        Usuario otro = otroUsuario();
        mvc.perform(con(sesion, get("/api/porteria/verificar").param("codigo", otro.getDocumento())))
                .andExpect(jsonPath("$.encontrado").value(true));
    }

    @CeladorYPorteria
    void registraUnaEntrada(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        Usuario otro = otroUsuario();
        mvc.perform(conJson(sesion, post("/api/porteria/movimientos"),
                        Map.of("tipoEntidad", "Usuario", "entidadId", otro.getId(), "tipo", "Entrada")))
                .andExpect(status().isCreated());
    }
}
