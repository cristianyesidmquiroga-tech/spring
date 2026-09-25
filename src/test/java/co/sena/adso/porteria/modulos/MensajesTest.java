package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Rol;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

// Portería 2: tests/modulos/test_mensajes.py
class MensajesTest extends PruebaIntegracion {

    @Value("${app.fotos.carpeta}")
    private String carpetaFotos;

    private Sesion sesionDe(Usuario u) throws Exception {
        return new Sesion(u, iniciarSesion(u.getCorreo(), CLAVE));
    }

    private Usuario persona(String correo, String documento) {
        return crearUsuario(correo, "Aprendiz", Rol.USUARIO, documento, u -> { });
    }

    private Usuario admin() {
        return crearUsuario("admin@sena.edu.co", "Administrador", Rol.ADMIN, "999", u -> { });
    }

    private long contar(String condicion, Object... valores) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM mensajes WHERE " + condicion, Long.class, valores);
    }

    private Map<String, Object> primero(Usuario persona) {
        return jdbc.queryForMap("SELECT * FROM mensajes WHERE usuario_id = ? ORDER BY id LIMIT 1", persona.getId());
    }

    private void enviar(Sesion sesion, String texto) throws Exception {
        mvc.perform(conJson(sesion, post("/api/mensajes"), Map.of("texto", texto)));
    }

    private void conFoto(Usuario persona) throws Exception {
        Path carpeta = Files.createDirectories(Path.of(carpetaFotos));
        String nombre = "user_" + persona.getId() + ".jpg";
        Files.write(carpeta.resolve(nombre), "prueba".getBytes(StandardCharsets.UTF_8));
        jdbc.update("UPDATE usuarios SET foto = ?, foto_estado = 'pendiente' WHERE id = ?", nombre, persona.getId());
    }

    @Nested
    class UsuarioEscribe {

        @Test
        void puedeEnviarUnMensaje() throws Exception {
            Usuario ana = persona("ana@sena.edu.co", "111");
            mvc.perform(conJson(sesionDe(ana), post("/api/mensajes"),
                    Map.of("texto", "No puedo subir mi foto, me sale error."))).andExpect(status().isCreated());
            Map<String, Object> mensaje = primero(ana);
            assertThat(mensaje.get("autor_id")).isEqualTo(ana.getId());
            assertThat(mensaje.get("autor_es_admin")).isEqualTo(false);
        }

        @Test
        void unMensajeVacioNoSeGuarda() throws Exception {
            enviar(sesionDe(persona("ana@sena.edu.co", "111")), "   ");
            assertThat(contar("TRUE")).isZero();
        }

        @Test
        void soloVeSuPropioHilo() throws Exception {
            Usuario ana = persona("ana@sena.edu.co", "111");
            enviar(sesionDe(persona("beto@sena.edu.co", "222")), "Mensaje de Beto");
            String cuerpo = mvc.perform(con(sesionDe(ana), get("/api/mensajes"))).andReturn().getResponse()
                    .getContentAsString(StandardCharsets.UTF_8);
            assertThat(cuerpo).doesNotContain("Mensaje de Beto");
        }

        @Test
        void noPuedeEntrarALaBandejaDelAdmin() throws Exception {
            mvc.perform(con(sesionDe(persona("ana@sena.edu.co", "111")), get("/api/bandeja")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void noPuedeEscribirleAOtroComoSiFueraAdmin() throws Exception {
            Usuario ana = persona("ana@sena.edu.co", "111");
            Usuario beto = persona("beto@sena.edu.co", "222");
            mvc.perform(conJson(sesionDe(ana), post("/api/bandeja/{id}", beto.getId()),
                    Map.of("texto", "Suplantando a un administrador"))).andExpect(status().isForbidden());
            assertThat(contar("TRUE")).isZero();
        }
    }

    @Nested
    class AdminResponde {

        @Test
        void elAdminEscribeAUnaPersona() throws Exception {
            Usuario admin = admin();
            Usuario ana = persona("ana@sena.edu.co", "111");
            mvc.perform(conJson(sesionDe(admin), post("/api/bandeja/{id}", ana.getId()),
                    Map.of("texto", "Te falta el tipo de sangre en el perfil."))).andExpect(status().isCreated());
            Map<String, Object> mensaje = primero(ana);
            assertThat(mensaje.get("autor_id")).isEqualTo(admin.getId());
            assertThat(mensaje.get("autor_es_admin")).isEqualTo(true);
        }

        @Test
        void laPersonaVeLoQueLeEscribioElAdmin() throws Exception {
            Usuario ana = persona("ana@sena.edu.co", "111");
            mvc.perform(conJson(sesionDe(admin()), post("/api/bandeja/{id}", ana.getId()),
                    Map.of("texto", "Revisa tu ficha, está mal escrita.")));
            String cuerpo = mvc.perform(con(sesionDe(ana), get("/api/mensajes"))).andReturn().getResponse()
                    .getContentAsString(StandardCharsets.UTF_8);
            assertThat(cuerpo).contains("Revisa tu ficha");
        }

        @Test
        void abrirElHiloMarcaLosMensajesComoLeidos() throws Exception {
            Usuario ana = persona("ana@sena.edu.co", "111");
            mvc.perform(conJson(sesionDe(admin()), post("/api/bandeja/{id}", ana.getId()), Map.of("texto", "Hola")));
            assertThat(contar("leido = false")).isEqualTo(1);
            mvc.perform(con(sesionDe(ana), get("/api/mensajes")));
            assertThat(contar("leido = false")).isZero();
        }

        @Test
        void escribirAAlguienQueNoExiste() throws Exception {
            mvc.perform(conJson(sesionDe(admin()), post("/api/bandeja/99999"), Map.of("texto", "Hola")))
                    .andExpect(status().isNotFound());
        }
    }

    // El correo se puede perder o ir a spam; el hilo vive dentro del sistema
    @Nested
    class RechazoDejaMensaje {

        @Test
        void alRechazarLaFotoQuedaElMotivoEnElHilo() throws Exception {
            Usuario ana = persona("ana@sena.edu.co", "111");
            conFoto(ana);
            String motivo = "El rostro está tapado por una gorra.";
            mvc.perform(conJson(sesionDe(admin()), post("/api/admin/fotos/{id}/revision", ana.getId()),
                    Map.of("aprobada", false, "motivo", motivo))).andExpect(status().isOk());

            Map<String, Object> mensaje = primero(ana);
            assertThat((String) mensaje.get("texto")).contains(motivo);
            assertThat(mensaje.get("automatico")).isEqualTo(true);
            assertThat(((String) mensaje.get("texto")).toLowerCase()).contains("respóndeme");
        }

        @Test
        void alAprobarTambienQuedaConstancia() throws Exception {
            Usuario ana = persona("ana@sena.edu.co", "111");
            conFoto(ana);
            mvc.perform(conJson(sesionDe(admin()), post("/api/admin/fotos/{id}/revision", ana.getId()),
                    Map.of("aprobada", true)));
            assertThat(((String) primero(ana).get("texto")).toLowerCase()).contains("aprobada");
        }
    }

    @Nested
    class CentroDeAyuda {

        @Test
        void cualquieraPuedeVerLasPreguntasFrecuentes() throws Exception {
            String cuerpo = mvc.perform(con(sesionDe(persona("ana@sena.edu.co", "111")), get("/api/ayuda")))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(cuerpo).contains("¿Por qué no me aparece el código de barras?");
            // "Hablar con un asesor" es el botón de React; la API entrega los asuntos con que se abre la conversación
            assertThat(cuerpo).contains("Problema con mi foto de perfil");
        }

        @Test
        void contactarAbreLaConversacion() throws Exception {
            Usuario ana = persona("ana@sena.edu.co", "111");
            mvc.perform(conJson(sesionDe(ana), post("/api/ayuda/contacto"), Map.of(
                    "asunto", "Problema con mi foto de perfil", "detalle", "Me dice que está borrosa pero se ve bien.")));
            String texto = (String) primero(ana).get("texto");
            assertThat(texto).contains("Problema con mi foto de perfil").contains("borrosa");
        }

        @Test
        void sinDetalleNoSeAbreNada() throws Exception {
            mvc.perform(conJson(sesionDe(persona("ana@sena.edu.co", "111")), post("/api/ayuda/contacto"),
                    Map.of("asunto", "Otro", "detalle", "  ")));
            assertThat(contar("TRUE")).isZero();
        }
    }

    // No solo el rol Admin: el personal administrativo atiende a los aprendices a diario
    @Nested
    class QuienPuedeAsesorar {

        @Test
        void elAdministrativoPuedeAsesorar() throws Exception {
            Usuario asesor = crearUsuario("adm@sena.edu.co", "Administrativo", Rol.USUARIO, "888", u -> { });
            assertThat(asesor.puedeAsesorar()).isTrue();
            mvc.perform(con(sesionDe(asesor), get("/api/bandeja"))).andExpect(status().isOk());
        }

        @Test
        void elAprendizNoPuedeAsesorar() {
            assertThat(persona("ap@sena.edu.co", "123").puedeAsesorar()).isFalse();
        }

        @Test
        void elCeladorNoPuedeAsesorar() {
            assertThat(crearUsuario("cel@sena.edu.co", "Celador", Rol.USUARIO, "777", u -> { }).puedeAsesorar()).isFalse();
        }

        @Test
        void elInstructorNoPuedeAsesorar() {
            assertThat(crearUsuario("ins@sena.edu.co", "Instructor", Rol.USUARIO, "666", u -> { }).puedeAsesorar())
                    .isFalse();
        }
    }
}
