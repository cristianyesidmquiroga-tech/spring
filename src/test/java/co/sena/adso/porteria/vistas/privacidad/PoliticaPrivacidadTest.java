package co.sena.adso.porteria.vistas.privacidad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/politica_privacidad/test_politica_privacidad.py
class PoliticaPrivacidadTest extends PruebaIntegracion {

    @Test
    void abreSinSesion() throws Exception {
        mvc.perform(get("/api/politica-privacidad")).andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void abreConSesion(Perfil perfil) throws Exception {
        mvc.perform(con(entrarComo(perfil), get("/api/politica-privacidad"))).andExpect(status().isOk());
    }
}
