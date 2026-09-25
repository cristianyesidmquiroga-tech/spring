package co.sena.adso.porteria.vistas.registro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.util.Map;
import org.junit.jupiter.api.Test;

// Portería 2: tests/vistas/registro/test_registro.py
class RegistroTest extends PruebaIntegracion {

    // Sin documento, como el formulario de la vista de Portería 2
    private Map<String, Object> datos() {
        Map<String, Object> datos = datosRegistro();
        datos.remove("documento");
        return datos;
    }

    // La página es de React; el formulario necesita el desafío abierto sin sesión
    @Test
    void laPaginaAbre() throws Exception {
        mvc.perform(get("/api/auth/captcha")).andExpect(status().isOk());
    }

    @Test
    void seRegistraComoAprendiz() throws Exception {
        registrar(datos());
        Usuario nuevo = porCorreo("nueva@sena.edu.co");
        assertThat(nuevo.getCargo()).isEqualTo("Aprendiz");
        assertThat(nuevo.getRol().getNombre()).isEqualTo("Usuario");
    }

    @Test
    void noPuedeElegirOtroCargo() throws Exception {
        Map<String, Object> datos = datos();
        datos.put("cargo", "Administrador");
        registrar(datos);
        Usuario nuevo = porCorreo("nueva@sena.edu.co");
        assertThat(nuevo == null || !"Administrador".equals(nuevo.getCargo())).isTrue();
    }

    @Test
    void contrasenasDistintasNoRegistran() throws Exception {
        Map<String, Object> datos = datos();
        datos.put("confirmacion", "Otra2026");
        registrar(datos);
        assertThat(porCorreo("nueva@sena.edu.co")).isNull();
    }

    @Test
    void correoRepetidoNoRegistra() throws Exception {
        crearUsuario("nueva@sena.edu.co", "123456");
        Map<String, Object> datos = datos();
        datos.put("nombre", "Otra Persona");
        registrar(datos);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE correo = 'nueva@sena.edu.co'", Long.class))
                .isEqualTo(1);
    }
}
