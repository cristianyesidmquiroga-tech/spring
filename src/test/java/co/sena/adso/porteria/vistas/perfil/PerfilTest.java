package co.sena.adso.porteria.vistas.perfil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/perfil/test_perfil.py
class PerfilTest extends PruebaIntegracion {

    private Usuario recargar(Sesion sesion) {
        return usuarioRepository.findById(sesion.usuario().getId()).orElseThrow();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void todosLosPerfilesEntran(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/perfil"))).andExpect(status().isOk());
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(get("/api/perfil")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void muestraSuNombre(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/perfil"))).andExpect(jsonPath("$.nombre").value(sesion.usuario().getNombre()));
    }

    @Test
    void documentoDeOtraPersonaSeRechaza() throws Exception {
        crearUsuario("otra.persona@sena.edu.co", "1098765432");
        Sesion sesion = entrarComo(Perfil.APRENDIZ);
        mvc.perform(conJson(sesion, put("/api/perfil"), Map.of("tipoDocumento", "CC", "documento", "1098765432")));
        assertThat(recargar(sesion).getDocumento()).isNotEqualTo("1098765432");
    }

    @Test
    void nombresConNumerosSeRechazan() throws Exception {
        Sesion sesion = entrarComo(Perfil.APRENDIZ);
        mvc.perform(conJson(sesion, put("/api/perfil"), Map.of("nombres", "Ana123")));
        assertThat(recargar(sesion).getNombres()).isNotEqualTo("Ana123");
    }
}
