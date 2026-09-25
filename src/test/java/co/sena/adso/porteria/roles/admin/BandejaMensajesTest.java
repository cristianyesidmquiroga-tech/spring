package co.sena.adso.porteria.roles.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.roles.PruebaRol;
import java.util.Map;
import org.junit.jupiter.api.Test;

// Portería 2: tests/roles/admin/test_bandeja_mensajes.py
class BandejaMensajesTest extends PruebaRol {

    @Test
    void entraALaBandeja() throws Exception {
        mvc.perform(con(sesion(), get("/api/bandeja"))).andExpect(status().isOk());
    }

    @Test
    void abreLaConversacionDeOtraPersona() throws Exception {
        mvc.perform(con(sesion(), get("/api/bandeja/{id}", otroUsuario().getId()))).andExpect(status().isOk());
    }

    @Test
    void respondeAOtraPersona() throws Exception {
        mvc.perform(conJson(sesion(), post("/api/bandeja/{id}", otroUsuario().getId()),
                Map.of("texto", "Hola, ya revisamos tu caso"))).andExpect(status().isCreated());
    }
}
