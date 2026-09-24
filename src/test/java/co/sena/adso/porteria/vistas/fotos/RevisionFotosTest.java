package co.sena.adso.porteria.vistas.fotos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/revision_fotos/test_revision_fotos.py
class RevisionFotosTest extends PruebaIntegracion {

    private static final Set<Perfil> ENTRAN = Set.of(Perfil.ADMIN);

    private Usuario otroConFotoPendiente() {
        return crearUsuario("otra.persona@sena.edu.co", "Aprendiz", "Usuario", "3000000002",
                u -> u.registrarFotoNueva("user_1.jpg", LocalDateTime.now()));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void accesoSegunElPerfil(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/admin/fotos")))
                .andExpect(ENTRAN.contains(perfil) ? status().isOk() : status().isForbidden());
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(get("/api/admin/fotos")).andExpect(status().isUnauthorized());
    }

    @Test
    void revisarSinPermisoDevuelve403() throws Exception {
        Usuario otro = crearUsuario("otra.persona@sena.edu.co", "3000000002");
        Sesion administrativo = entrarComo(Perfil.ADMINISTRATIVO);
        mvc.perform(conJson(administrativo, post("/api/admin/fotos/" + otro.getId() + "/revision"),
                        Map.of("aprobada", true)))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechazarSinMotivoSeRechaza() throws Exception {
        Usuario otro = otroConFotoPendiente();
        Sesion admin = entrarComo(Perfil.ADMIN);
        mvc.perform(conJson(admin, post("/api/admin/fotos/" + otro.getId() + "/revision"),
                        Map.of("aprobada", false, "motivo", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void apruebaUnaFoto() throws Exception {
        Usuario otro = otroConFotoPendiente();
        Sesion admin = entrarComo(Perfil.ADMIN);
        mvc.perform(conJson(admin, post("/api/admin/fotos/" + otro.getId() + "/revision"), Map.of("aprobada", true)));
        assertThat(usuarioRepository.findById(otro.getId()).orElseThrow().getFotoEstado()).isEqualTo("aprobada");
    }
}
