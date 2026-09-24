package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.repository.FichaRepository;
import co.sena.adso.porteria.repository.UsuarioRepository;
import co.sena.adso.porteria.service.AuthService;
import co.sena.adso.porteria.service.AvatarService;
import co.sena.adso.porteria.service.CarnetService;
import co.sena.adso.porteria.service.DocumentoService;
import co.sena.adso.porteria.service.FotoService;
import co.sena.adso.porteria.service.PerfilService;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

// Portería 2: tests/modulos/test_acceso_fotos.py
class AccesoFotosTest extends PruebaIntegracion {

    @Value("${app.fotos.carpeta}")
    private String carpetaFotos;

    private final AuthService auth = mock(AuthService.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final FotoService fotos = mock(FotoService.class);
    private final PerfilService regla = new PerfilService(auth, usuarios, mock(FichaRepository.class),
            mock(DocumentoService.class), mock(CarnetService.class), fotos, new AvatarService(), Clock.systemDefaultZone());

    @Test
    void laCarpetaPublicaYaNoSirveFotos() throws Exception {
        int estado = mvc.perform(get("/static/uploads/profiles/user_1.jpg")).andReturn().getResponse().getStatus();
        assertThat(estado).isNotEqualTo(200);
    }

    @Test
    void laVistaDeFotosExigeSesion() throws Exception {
        Usuario conFoto = conFoto();
        mvc.perform(get("/api/usuarios/{id}/foto", conFoto.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    void noConfirmaSiLaPersonaExiste() throws Exception {
        int existente = mvc.perform(get("/api/usuarios/1/foto")).andReturn().getResponse().getStatus();
        int inventado = mvc.perform(get("/api/usuarios/999999/foto")).andReturn().getResponse().getStatus();
        assertThat(existente).isEqualTo(inventado);
    }

    @Test
    void cadaQuienVeLaSuya() throws Exception {
        Usuario conFoto = conFoto();
        Sesion sesion = new Sesion(conFoto, iniciarSesion(conFoto.getCorreo(), CLAVE));
        mvc.perform(con(sesion, get("/api/usuarios/{id}/foto", conFoto.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    void unAprendizNoVeLaDeOtro() throws Exception {
        Usuario conFoto = conFoto();
        Long otroId = crearUsuario("otra@sena.edu.co", "1122334455").getId();
        Sesion sesion = new Sesion(conFoto, iniciarSesion(conFoto.getCorreo(), CLAVE));
        // Flask respondía 404; en la API la falta de permiso se traduce a 403
        mvc.perform(con(sesion, get("/api/usuarios/{id}/foto", otroId))).andExpect(status().isForbidden());
    }

    @Test
    void nadieSinAutenticar() {
        when(auth.usuarioActual()).thenThrow(new AuthenticationCredentialsNotFoundException("Sin sesión"));
        assertThatThrownBy(() -> regla.foto(1L)).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        verify(fotos, never()).buscar(any());
    }

    @Test
    void unoMismoSi() {
        Usuario espectador = espectador(null);
        when(auth.usuarioActual()).thenReturn(espectador);
        when(usuarios.findById(7L)).thenReturn(Optional.of(espectador));
        regla.foto(7L);
        verify(fotos).buscar(espectador);
    }

    @Test
    void reglaUnAprendizNoVeLaDeOtro() {
        Usuario espectador = espectador(null);
        when(auth.usuarioActual()).thenReturn(espectador);
        assertThatThrownBy(() -> regla.foto(8L)).isInstanceOf(AccessDeniedException.class);
        verify(fotos, never()).buscar(any());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"es_admin", "puede_operar_porteria", "puede_asesorar", "puede_gestionar_asistencia"})
    void quienLaNecesitaPorSuFuncionSi(String permiso) {
        Usuario dueno = mock(Usuario.class);
        Usuario espectador = espectador(permiso);
        when(auth.usuarioActual()).thenReturn(espectador);
        when(usuarios.findById(8L)).thenReturn(Optional.of(dueno));
        regla.foto(8L);
        verify(fotos).buscar(dueno);
    }

    private Usuario conFoto() throws Exception {
        Usuario usuario = crearUsuario();
        String nombre = "user_" + usuario.getId() + ".jpg";
        Path carpeta = Path.of(carpetaFotos);
        Files.createDirectories(carpeta);
        Files.write(carpeta.resolve(nombre), new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0, 1, 2});
        jdbc.update("UPDATE usuarios SET foto = ? WHERE id = ?", nombre, usuario.getId());
        return usuario;
    }

    private static Usuario espectador(String permiso) {
        Usuario u = mock(Usuario.class);
        when(u.getId()).thenReturn(7L);
        when(u.esAdmin()).thenReturn("es_admin".equals(permiso));
        when(u.puedeOperarPorteria()).thenReturn("puede_operar_porteria".equals(permiso));
        when(u.puedeAsesorar()).thenReturn("puede_asesorar".equals(permiso));
        when(u.puedeGestionarAsistencia()).thenReturn("puede_gestionar_asistencia".equals(permiso));
        return u;
    }
}
