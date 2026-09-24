package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Rol;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.repository.FichaRepository;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.ResultActions;

// Portería 2: tests/modulos/test_revision_fotos.py
class RevisionFotosTest extends PruebaIntegracion {

    @Value("${app.fotos.carpeta}")
    private String carpetaFotos;

    @Autowired
    private FichaRepository fichaRepository;

    @Test
    void unAprendizNoEntraALaCola() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        mvc.perform(con(aprendiz, get("/api/admin/fotos"))).andExpect(status().isForbidden());
    }

    @Test
    void unCeladorTampoco() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        mvc.perform(con(celador, get("/api/admin/fotos"))).andExpect(status().isForbidden());
    }

    @Test
    void unAprendizNoPuedeAprobarSuPropiaFoto() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        ponerFoto(aprendiz.usuario(), Usuario.FOTO_PENDIENTE);
        mvc.perform(conJson(aprendiz, post("/api/admin/fotos/{id}/revision", aprendiz.usuario().getId()),
                        Map.of("aprobada", true)))
                .andExpect(status().isForbidden());
        assertThat(estadoFoto(aprendiz.usuario())).isEqualTo(Usuario.FOTO_PENDIENTE);
    }

    @Test
    void elAdminVeLaCola() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        mvc.perform(con(admin, get("/api/admin/fotos"))).andExpect(status().isOk());
    }

    @Test
    void aprobarDejaLaFotoAprobadaYAudita() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        Usuario persona = personaConFotoPendiente();
        Path ruta = ponerFoto(persona, Usuario.FOTO_PENDIENTE);

        mvc.perform(conJson(admin, post("/api/admin/fotos/{id}/revision", persona.getId()), Map.of("aprobada", true)))
                .andExpect(status().isOk());

        Map<String, Object> fila = jdbc.queryForMap(
                "SELECT foto_estado, foto_revisada_por, foto_fecha_revision FROM usuarios WHERE id = ?", persona.getId());
        assertThat(fila.get("foto_estado")).isEqualTo(Usuario.FOTO_APROBADA);
        assertThat(((Number) fila.get("foto_revisada_por")).longValue()).isEqualTo(admin.usuario().getId());
        assertThat(fila.get("foto_fecha_revision")).isNotNull();
        assertThat(ruta).isRegularFile();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria WHERE accion = 'Foto aprobada'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void rechazarExigeMotivo() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        Usuario persona = personaConFotoPendiente();
        ponerFoto(persona, Usuario.FOTO_PENDIENTE);

        mvc.perform(conJson(admin, post("/api/admin/fotos/{id}/revision", persona.getId()),
                        Map.of("aprobada", false, "motivo", "   ")))
                .andExpect(status().isBadRequest());
        assertThat(estadoFoto(persona)).isEqualTo(Usuario.FOTO_PENDIENTE);
    }

    @Test
    void rechazarBorraLaFotoYGuardaElMotivo() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        Usuario persona = personaConFotoPendiente();
        Path ruta = ponerFoto(persona, Usuario.FOTO_PENDIENTE);
        String motivo = "La foto no corresponde a la persona registrada.";

        mvc.perform(conJson(admin, post("/api/admin/fotos/{id}/revision", persona.getId()),
                        Map.of("aprobada", false, "motivo", motivo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.pendientes").value(0));

        Map<String, Object> fila = jdbc.queryForMap(
                "SELECT foto, foto_estado, foto_motivo, perfil_completo FROM usuarios WHERE id = ?", persona.getId());
        assertThat(fila.get("foto_estado")).isEqualTo(Usuario.FOTO_RECHAZADA);
        assertThat(fila.get("foto_motivo")).isEqualTo(motivo);
        assertThat(fila.get("perfil_completo")).isEqualTo(false);
        assertThat(fila.get("foto")).isNull();
        assertThat(ruta).doesNotExist();
    }

    @Test
    void decisionInvalidaSeRechaza() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        Usuario persona = personaConFotoPendiente();
        ponerFoto(persona, Usuario.FOTO_PENDIENTE);

        mvc.perform(conJson(admin, post("/api/admin/fotos/{id}/revision", persona.getId()),
                        Map.of("aprobada", "aprobar_a_medias")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void usuarioInexistente() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        mvc.perform(conJson(admin, post("/api/admin/fotos/99999/revision"), Map.of("aprobada", true)))
                .andExpect(status().isNotFound());
    }

    @Test
    void sinAprobarNoHayCarnet() throws Exception {
        Sesion persona = aprendizSinCarnet();
        ponerFoto(persona.usuario(), Usuario.FOTO_PENDIENTE);

        subirDatos(persona).andExpect(jsonPath("$.perfilCompleto").value(false));
    }

    @Test
    void conLaFotoAprobadaSiHayCarnet() throws Exception {
        Sesion persona = aprendizSinCarnet();
        ponerFoto(persona.usuario(), Usuario.FOTO_APROBADA);

        subirDatos(persona).andExpect(jsonPath("$.perfilCompleto").value(true));
        mvc.perform(con(persona, get("/api/perfil/carnet")))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.codigoBarras").value("1098765432"));
    }

    @Test
    void unUsuarioNuevoEmpiezaSinFoto() throws Exception {
        Sesion nueva = entrarComo(Perfil.APRENDIZ);
        mvc.perform(con(nueva, get("/api/perfil")))
                .andExpect(jsonPath("$.fotoEstado").value(Usuario.FOTO_SIN_FOTO))
                .andExpect(jsonPath("$.tieneFoto").value(false));
    }

    @Test
    void laApiDelEscanerDiceSiLaFotoEstaAprobada() throws Exception {
        Sesion celador = entrarComo(Perfil.CELADOR);
        Usuario persona = crearUsuario("ana@sena.edu.co", "111");
        ponerFoto(persona, Usuario.FOTO_PENDIENTE);

        mvc.perform(con(celador, get("/api/porteria/verificar").param("codigo", "111")))
                .andExpect(jsonPath("$.encontrado").value(true))
                .andExpect(jsonPath("$.fotoAprobada").value(false));

        jdbc.update("UPDATE usuarios SET foto_estado = ? WHERE id = ?", Usuario.FOTO_APROBADA, persona.getId());
        mvc.perform(con(celador, get("/api/porteria/verificar").param("codigo", "111")))
                .andExpect(jsonPath("$.fotoAprobada").value(true));
    }

    @Test
    void un404NoRevientaParaUnUsuarioAutenticado() throws Exception {
        Sesion admin = entrarComo(Perfil.ADMIN);
        mvc.perform(con(admin, get("/esta-ruta-no-existe")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    private Usuario personaConFotoPendiente() {
        return crearUsuario("ana@sena.edu.co", "Aprendiz", Rol.USUARIO, "111", u -> u.setPerfilCompleto(false));
    }

    private Sesion aprendizSinCarnet() throws Exception {
        Usuario u = crearUsuario("ana@sena.edu.co", "Aprendiz", Rol.USUARIO, "111", p -> p.setPerfilCompleto(false));
        return new Sesion(u, iniciarSesion(u.getCorreo(), CLAVE));
    }

    // El aprendiz ya no teclea programa ni ficha: elige una ficha y hereda de ella el resto
    private ResultActions subirDatos(Sesion persona) throws Exception {
        Map<String, Object> datos = new HashMap<>();
        datos.put("documento", "1098765432");
        datos.put("tipoDocumento", "CC");
        datos.put("tipoSangre", "O+");
        datos.put("fichaId", fichaRepository.findByNumero("2977385").orElseThrow().getId());
        return mvc.perform(conJson(persona, put("/api/perfil"), datos)).andExpect(status().isOk());
    }

    private Path ponerFoto(Usuario usuario, String estado) throws Exception {
        String nombre = "user_" + usuario.getId() + ".jpg";
        Path ruta = Files.createDirectories(Path.of(carpetaFotos)).resolve(nombre);
        Files.write(ruta, "contenido de prueba".getBytes(StandardCharsets.UTF_8));
        jdbc.update("UPDATE usuarios SET foto = ?, foto_estado = ? WHERE id = ?", nombre, estado, usuario.getId());
        return ruta;
    }

    private String estadoFoto(Usuario usuario) {
        return jdbc.queryForObject("SELECT foto_estado FROM usuarios WHERE id = ?", String.class, usuario.getId());
    }
}
