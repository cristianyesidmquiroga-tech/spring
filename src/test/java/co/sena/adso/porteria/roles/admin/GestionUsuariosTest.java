package co.sena.adso.porteria.roles.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import java.util.Map;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/admin/test_gestion_usuarios.py
class GestionUsuariosTest extends PruebaRol {

    @Test
    void entraAGestionDeUsuarios() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(con(sesion, get("/api/admin/usuarios"))).andExpect(status().isOk());
    }

    @Test
    void creaUnUsuario() throws Exception {
        Sesion sesion = sesion();
        Long rol = jdbc.queryForObject("SELECT id FROM roles WHERE nombre = 'Usuario'", Long.class);
        mvc.perform(conJson(sesion, post("/api/admin/usuarios"), Map.of("nombre", "Nueva Persona",
                        "correo", "nueva@sena.edu.co", "contrasena", "Temporal2026", "rolId", rol, "cargo", "Aprendiz")))
                .andExpect(status().isCreated());
        assertThat(usuarioRepository.findAll()).anyMatch(u -> u.getCorreo().equals("nueva@sena.edu.co"));
    }

    // Portería 2 respondía 400; en Spring una regla de negocio es 422
    @Test
    void noPuedeEliminarseASiMismo() throws Exception {
        Sesion sesion = sesion();
        mvc.perform(conJson(sesion, delete("/api/admin/usuarios/{id}", sesion.usuario().getId()), Map.of()))
                .andExpect(status().isUnprocessableEntity());
    }
}
