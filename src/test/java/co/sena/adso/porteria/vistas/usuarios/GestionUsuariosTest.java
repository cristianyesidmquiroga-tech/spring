package co.sena.adso.porteria.vistas.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Portería 2: tests/vistas/gestion_usuarios/test_gestion_usuarios.py
class GestionUsuariosTest extends PruebaIntegracion {

    private static final Set<Perfil> ENTRAN = Set.of(Perfil.ADMIN);

    private Long rol(String nombre) {
        return jdbc.queryForObject("SELECT id FROM roles WHERE nombre = ?", Long.class, nombre);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Perfil.class)
    void accesoSegunElPerfil(Perfil perfil) throws Exception {
        Sesion sesion = entrarComo(perfil);
        mvc.perform(con(sesion, get("/api/admin/usuarios")))
                .andExpect(ENTRAN.contains(perfil) ? status().isOk() : status().isForbidden());
    }

    @Test
    void sinSesionPideLogin() throws Exception {
        mvc.perform(get("/api/admin/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void creaUnUsuarioConContrasenaTemporal() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        mvc.perform(conJson(admin, post("/api/admin/usuarios"), Map.of("nombre", "Nueva Persona",
                "correo", "Nueva@sena.edu.co", "contrasena", "Temporal2026", "rolId", rol("Usuario"), "cargo", "Aprendiz")));
        assertThat(jdbc.queryForObject("SELECT debe_cambiar_contrasena FROM usuarios WHERE correo = ?", Boolean.class,
                "nueva@sena.edu.co")).isTrue();
    }

    @Test
    void cargoInvalidoSeRechaza() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        mvc.perform(conJson(admin, post("/api/admin/usuarios"), Map.of("nombre", "X", "correo", "x@sena.edu.co",
                        "contrasena", "Temporal2026", "rolId", rol("Usuario"), "cargo", "Rey")))
                .andExpect(status().isBadRequest());
    }

    // En la API editar y desbloquear son dos endpoints; en Flask era un solo PUT
    @Test
    void editaYDesbloquea() throws Exception {
        Usuario otro = crearUsuario("otra.persona@sena.edu.co", "Aprendiz", "Usuario", "3000000002", u -> {
            for (int i = 0; i < 5; i++) {
                u.registrarIntentoFallido(Integer.MAX_VALUE, null);
            }
        });
        Sesion admin = entrarComo(Perfil.ADMIN);
        mvc.perform(conJson(admin, put("/api/admin/usuarios/" + otro.getId()), Map.of("nombre", "Nombre Nuevo",
                "correo", otro.getCorreo(), "rolId", rol("Usuario"), "cargo", "Aprendiz",
                "tipoDocumento", "CC", "documento", "3000000002")));
        mvc.perform(con(admin, post("/api/admin/usuarios/" + otro.getId() + "/desbloquear")));
        Usuario recargado = usuarioRepository.findById(otro.getId()).orElseThrow();
        assertThat(recargado.getNombre()).isEqualTo("Nombre Nuevo");
        assertThat(recargado.getIntentosFallidos()).isZero();
    }

    @Test
    void eliminaUnUsuario() throws Exception {
        Usuario otro = crearUsuario("otra.persona@sena.edu.co", "3000000002");
        Sesion admin = entrarComo(Perfil.ADMIN);
        mvc.perform(conJson(admin, delete("/api/admin/usuarios/" + otro.getId()), Map.of()));
        assertThat(usuarioRepository.findById(otro.getId())).isEmpty();
    }

    @Test
    void noEliminaOtroAdmin() throws Exception {
        Usuario otro = crearUsuario("otro.admin@sena.edu.co", "Administrador", "Admin", "3000000003", u -> { });
        Sesion admin = entrarComo(Perfil.ADMIN);
        mvc.perform(conJson(admin, delete("/api/admin/usuarios/" + otro.getId()), Map.of()))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiSinPermisoDevuelve403() throws Exception {
        Sesion administrador = entrarComo(Perfil.ADMINISTRADOR);
        mvc.perform(conJson(administrador, post("/api/admin/usuarios"), Map.of())).andExpect(status().isForbidden());
    }
}
