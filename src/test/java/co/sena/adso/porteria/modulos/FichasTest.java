package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Ficha;
import co.sena.adso.porteria.entity.Rol;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.repository.FichaRepository;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// Portería 2: tests/modulos/test_fichas.py
class FichasTest extends PruebaIntegracion {

    private static final String RUTA = "/api/admin/fichas";
    private static final String ADSO = "Análisis y Desarrollo de Software";

    @Autowired
    private FichaRepository fichaRepository;

    private Ficha ficha() {
        return fichaRepository.save(new Ficha("2847513", ADSO, LocalDate.of(2027, 6, 30)));
    }

    private static Usuario aprendiz() {
        Rol rol = BeanUtils.instantiateClass(Rol.class);
        ReflectionTestUtils.setField(rol, "nombre", Rol.USUARIO);
        return new Usuario("Aprendiz", "a@sena.edu.co", "hash", rol, "Aprendiz");
    }

    private Sesion admin() throws Exception {
        Usuario admin = crearUsuario("admin@sena.edu.co", "Administrador", Rol.ADMIN, "900", u -> { });
        return new Sesion(admin, iniciarSesion(admin.getCorreo(), CLAVE));
    }

    private Sesion aprendizConSesion() throws Exception {
        Usuario u = crearUsuario("aprendiz@sena.edu.co", "1010");
        return new Sesion(u, iniciarSesion(u.getCorreo(), CLAVE));
    }

    private static Map<String, Object> datos(String numero, String programa, String fecha) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("numero", numero);
        datos.put("programa", programa);
        datos.put("fechaFinalizacion", fecha);
        return datos;
    }

    private Usuario recargar(Usuario u) {
        return usuarioRepository.findById(u.getId()).orElseThrow();
    }

    private Long fichaIdDe(Usuario u) {
        return jdbc.queryForObject("SELECT ficha_id FROM usuarios WHERE id = ?", Long.class, u.getId());
    }

    @Nested
    class HerenciaDesdeLaFicha {

        @Test
        void elAprendizHeredaProgramaYFecha() {
            Usuario persona = aprendiz();
            persona.asignarFicha(new Ficha("2847513", ADSO, LocalDate.of(2027, 6, 30)));
            assertThat(persona.programaCarnet()).isEqualTo(ADSO);
            assertThat(persona.fechaFinalizacionCarnet()).isEqualTo("30/06/2027");
            assertThat(persona.numeroFicha()).isEqualTo("2847513");
        }

        @Test
        void cambiarLaFichaCambiaElCarnetDeTodosSusAprendices() {
            Ficha ficha = new Ficha("2847513", ADSO, LocalDate.of(2027, 6, 30));
            Usuario una = aprendiz();
            Usuario otra = aprendiz();
            una.asignarFicha(ficha);
            otra.asignarFicha(ficha);

            ficha.actualizar("2847513", ADSO, LocalDate.of(2028, 1, 15));

            assertThat(una.fechaFinalizacionCarnet()).isEqualTo("15/01/2028");
            assertThat(otra.fechaFinalizacionCarnet()).isEqualTo("15/01/2028");
        }

        // Quien tenía la ficha escrita a mano no debe quedarse sin carnet
        @Test
        void sinFichaEnlazadaSeUsaElTextoHistorico() {
            Usuario persona = aprendiz();
            persona.setFicha("2999999");
            persona.setPrograma("Sistemas");
            assertThat(persona.numeroFicha()).isEqualTo("2999999");
            assertThat(persona.programaCarnet()).isEqualTo("Sistemas");
            assertThat(persona.fechaFinalizacionCarnet()).isEmpty();
        }

        @Test
        void unaFichaSinFechaNoRevienta() {
            Usuario persona = aprendiz();
            persona.asignarFicha(new Ficha("111222", "Contabilidad", null));
            assertThat(persona.fechaFinalizacionCarnet()).isEmpty();
        }
    }

    @Nested
    class ElAprendizEligeFichaEnSuPerfil {

        @Test
        void alElegirFichaSeCopianNumeroYPrograma() throws Exception {
            Ficha ficha = ficha();
            Sesion aprendiz = aprendizConSesion();
            mvc.perform(conJson(aprendiz, put("/api/perfil"), Map.of("fichaId", ficha.getId())))
                    .andExpect(status().isOk());

            Usuario persona = recargar(aprendiz.usuario());
            assertThat(fichaIdDe(persona)).isEqualTo(ficha.getId());
            assertThat(persona.getFicha()).isEqualTo("2847513");
            assertThat(persona.getPrograma()).isEqualTo(ADSO);
        }

        @Test
        void unaFichaInexistenteSeRechaza() throws Exception {
            Sesion aprendiz = aprendizConSesion();
            mvc.perform(conJson(aprendiz, put("/api/perfil"), Map.of("fichaId", 99999)))
                    .andExpect(status().isBadRequest());
            assertThat(fichaIdDe(aprendiz.usuario())).isNull();
        }

        // El programa viene de la ficha; enviarlo a mano no debe pisarlo
        @Test
        void elAprendizNoPuedeEscribirSuPrograma() throws Exception {
            Ficha ficha = ficha();
            Sesion aprendiz = aprendizConSesion();
            mvc.perform(conJson(aprendiz, put("/api/perfil"), Map.of("fichaId", ficha.getId(), "programa", "Otra Cosa")));
            assertThat(recargar(aprendiz.usuario()).getPrograma()).isEqualTo(ADSO);
        }
    }

    // Quien edita una ficha cambia el carnet de todos sus aprendices: la pantalla es solo para administradores
    @Nested
    class PermisosDeLaPantallaDeFichas {

        @Test
        void unAdminEntra() throws Exception {
            mvc.perform(con(admin(), get(RUTA))).andExpect(status().isOk());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"Aprendiz", "Instructor", "Celador", "Administrativo", "Administrador"})
        void quienNoEsAdminNoEntra(String cargo) throws Exception {
            Usuario u = crearUsuario(cargo.toLowerCase() + "@sena.edu.co", cargo, Rol.USUARIO, "77" + cargo.length(),
                    x -> { });
            Sesion sesion = new Sesion(u, iniciarSesion(u.getCorreo(), CLAVE));
            mvc.perform(con(sesion, get(RUTA))).andExpect(status().isForbidden());
        }

        @Test
        void sinSesionRedirigeAlLogin() throws Exception {
            mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({"crear, POST, /api/admin/fichas", "editar, PUT, /api/admin/fichas/1",
                "archivar, PATCH, /api/admin/fichas/1/archivar"})
        void lasAccionesDeEscrituraTambienExigenAdmin(String accion, String metodo, String url) throws Exception {
            MockHttpServletRequestBuilder peticion = switch (metodo) {
                case "POST" -> post(url);
                case "PUT" -> put(url);
                default -> patch(url);
            };
            mvc.perform(conJson(aprendizConSesion(), peticion, datos("1234", "Cualquiera", null)))
                    .andExpect(status().isForbidden());
            assertThat(fichaRepository.findByNumero("1234")).isEmpty();
        }
    }

    @Nested
    class GestionDeFichas {

        @Test
        void crearEditarYArchivar() throws Exception {
            Sesion admin = admin();
            mvc.perform(conJson(admin, post(RUTA), datos("3141592", "Cocina", "2027-12-01")))
                    .andExpect(status().isCreated());
            Ficha creada = fichaRepository.findByNumero("3141592").orElseThrow();
            assertThat(creada.getFechaFinalizacion()).isEqualTo(LocalDate.of(2027, 12, 1));
            assertThat(creada.isActiva()).isTrue();

            mvc.perform(conJson(admin, put(RUTA + "/{id}", creada.getId()), datos("3141592", "Gastronomía", "2028-03-15")))
                    .andExpect(status().isOk());
            creada = fichaRepository.findById(creada.getId()).orElseThrow();
            assertThat(creada.getPrograma()).isEqualTo("Gastronomía");
            assertThat(creada.getFechaFinalizacion()).isEqualTo(LocalDate.of(2028, 3, 15));

            mvc.perform(con(admin, patch(RUTA + "/{id}/archivar", creada.getId()))).andExpect(status().isOk());
            assertThat(fichaRepository.findById(creada.getId()).orElseThrow().isActiva()).isFalse();
        }

        @Test
        void noSeAdmitenNumerosDeFichaInvalidos() throws Exception {
            Sesion admin = admin();
            long antes = fichaRepository.count();
            for (String numero : new String[] {"abc", "12", "", "12345678901234"}) {
                mvc.perform(conJson(admin, post(RUTA), datos(numero, "Cocina", null))).andExpect(status().isBadRequest());
            }
            assertThat(fichaRepository.count()).isEqualTo(antes);
        }

        @Test
        void noSeRepiteElNumeroDeFicha() throws Exception {
            ficha();
            mvc.perform(conJson(admin(), post(RUTA), datos("2847513", "Duplicada", null)))
                    .andExpect(status().isConflict());
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM fichas WHERE numero = '2847513'", Long.class))
                    .isEqualTo(1);
        }

        @Test
        void unaFechaInvalidaNoCreaLaFicha() throws Exception {
            mvc.perform(conJson(admin(), post(RUTA), datos("4567890", "Cocina", "31/12/2027")))
                    .andExpect(status().isBadRequest());
            assertThat(fichaRepository.findByNumero("4567890")).isEmpty();
        }

        @Test
        void archivarNoBorraLaFichaDeSusAprendices() throws Exception {
            Ficha ficha = ficha();
            Usuario aprendiz = crearUsuario("aprendiz@sena.edu.co", "Aprendiz", Rol.USUARIO, "1010",
                    u -> u.asignarFicha(ficha));

            mvc.perform(con(admin(), patch(RUTA + "/{id}/archivar", ficha.getId()))).andExpect(status().isOk());

            assertThat(fichaIdDe(aprendiz)).isEqualTo(ficha.getId());
            assertThat(jdbc.queryForObject("SELECT f.programa FROM usuarios u JOIN fichas f ON f.id = u.ficha_id "
                    + "WHERE u.id = ?", String.class, aprendiz.getId())).isEqualTo(ADSO);
        }
    }

    // El administrador pasa la ficha como texto: si ya está registrada, el aprendiz hereda de una vez
    @Nested
    class AltaDesdeAdministracion {

        @Test
        void alCrearUsuarioSeEnlazaLaFichaExistente() throws Exception {
            Ficha ficha = ficha();
            Long rolUsuario = jdbc.queryForObject("SELECT id FROM roles WHERE nombre = 'Usuario'", Long.class);
            mvc.perform(conJson(admin(), post("/api/admin/usuarios"), Map.of("nombre", "Nueva Aprendiz",
                    "correo", "nueva@sena.edu.co", "contrasena", "ClaveLarga2026", "rolId", rolUsuario,
                    "cargo", "Aprendiz", "documento", "404040", "ficha", "2847513")))
                    .andExpect(status().isCreated());

            Map<String, Object> nueva = jdbc.queryForMap("SELECT u.ficha_id, f.programa, f.fecha_finalizacion "
                    + "FROM usuarios u JOIN fichas f ON f.id = u.ficha_id WHERE u.correo = 'nueva@sena.edu.co'");
            assertThat(nueva.get("ficha_id")).isEqualTo(ficha.getId());
            assertThat(nueva.get("programa")).isEqualTo(ADSO);
            assertThat(nueva.get("fecha_finalizacion").toString()).isEqualTo("2027-06-30");
        }

        // Inventar un programa a partir de un número suelto sería peor que dejar el carnet sin fecha
        @Test
        void unaFichaNoRegistradaNoSeInventa() throws Exception {
            Long rolUsuario = jdbc.queryForObject("SELECT id FROM roles WHERE nombre = 'Usuario'", Long.class);
            mvc.perform(conJson(admin(), post("/api/admin/usuarios"), Map.of("nombre", "Otra Aprendiz",
                    "correo", "otra@sena.edu.co", "contrasena", "ClaveLarga2026", "rolId", rolUsuario,
                    "cargo", "Aprendiz", "documento", "505050", "ficha", "8888888")))
                    .andExpect(status().isCreated());
            assertThat(jdbc.queryForObject("SELECT ficha_id FROM usuarios WHERE correo = 'otra@sena.edu.co'", Long.class))
                    .isNull();
            assertThat(fichaRepository.findByNumero("8888888")).isEmpty();
        }
    }
}
