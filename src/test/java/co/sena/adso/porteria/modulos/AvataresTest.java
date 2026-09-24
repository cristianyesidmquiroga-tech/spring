package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.service.AvatarService;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;

// Portería 2: tests/modulos/test_avatares.py
class AvataresTest extends PruebaIntegracion {

    private static final Path CARPETA_AVATARES = Path.of("src/main/resources/avatares");

    private final AvatarService avatares = new AvatarService();

    @Value("${app.fotos.carpeta}")
    private String carpetaFotos;

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "Aprendiz, aprendiz.svg",
            "Instructor, instructor.svg",
            "Celador, celador.svg",
            "Administrador, administrador.svg",
            "Administrativo, administrativo.svg"})
    void cadaCargoTieneElSuyo(String cargo, String esperado) {
        assertThat(avatares.archivoDe(cargo)).isEqualTo(esperado);
    }

    @Test
    void noDistingueMayusculasNiEspacios() {
        assertThat(avatares.archivoDe("  aPrEnDiZ  ")).isEqualTo("aprendiz.svg");
    }

    @Test
    void porteriaComparteAvatarConCelador() {
        assertThat(avatares.archivoDe("Portería")).isEqualTo(avatares.archivoDe("Celador"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @NullAndEmptySource
    @ValueSource(strings = {"Cargo Inventado", "xyz"})
    void cargoDesconocidoCaeAlGenerico(String cargo) {
        assertThat(avatares.archivoDe(cargo)).isEqualTo(AvatarService.GENERICO);
    }

    @Test
    void todosLosArchivosExisten() {
        for (String cargo : Arrays.asList("Aprendiz", "Instructor", "Celador", "Administrador", "Administrativo",
                "Visitante", "Vehiculo", "ObjetoExterno", null)) {
            assertThat(new ClassPathResource("avatares/" + avatares.archivoDe(cargo)).exists())
                    .as("falta el archivo de %s", cargo).isTrue();
        }
    }

    // Un SVG admite <script>; estos se sirven desde el propio dominio, así que deben ser solo gráficos
    @Test
    void losSvgNoTraenScripts() throws IOException {
        try (Stream<Path> archivos = Files.list(CARPETA_AVATARES)) {
            for (Path archivo : archivos.toList()) {
                String contenido = Files.readString(archivo, StandardCharsets.UTF_8).toLowerCase();
                assertThat(contenido).as(archivo.toString()).doesNotContain("<script", "onload", "href");
            }
        }
    }

    @Test
    void sinFotoDevuelveElAvatarDelCargo() throws Exception {
        Sesion instructor = entrarComo(Perfil.INSTRUCTOR);
        mvc.perform(con(instructor, get("/api/usuarios/{id}/foto", instructor.usuario().getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/svg+xml"))
                .andExpect(content().bytes(bytesDe("instructor.svg")));
    }

    @Test
    void fotoInexistenteCaeAlAvatar() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        jdbc.update("UPDATE usuarios SET foto = 'no_existe.jpg' WHERE id = ?", aprendiz.usuario().getId());
        mvc.perform(con(aprendiz, get("/api/usuarios/{id}/foto", aprendiz.usuario().getId())))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytesDe("aprendiz.svg")));
    }

    @Test
    void fotoExistenteSeUsa() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        Long id = aprendiz.usuario().getId();
        Path carpeta = Files.createDirectories(Path.of(carpetaFotos));
        Files.write(carpeta.resolve("user_" + id + ".jpg"), "contenido".getBytes(StandardCharsets.UTF_8));
        jdbc.update("UPDATE usuarios SET foto = ? WHERE id = ?", "user_" + id + ".jpg", id);
        mvc.perform(con(aprendiz, get("/api/usuarios/{id}/foto", id)))
                .andExpect(status().isOk())
                .andExpect(content().bytes("contenido".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void ningunaPlantillaLlamaAUiAvatars() throws IOException {
        List<Path> culpables;
        try (Stream<Path> rutas = Files.walk(Path.of("src/main"))) {
            culpables = rutas.filter(Files::isRegularFile).filter(AvataresTest::mencionaUiAvatars).toList();
        }
        assertThat(culpables).as("todavía usan ui-avatars.com").isEmpty();
    }

    @Test
    void elCspNoPermiteUiAvatars() throws Exception {
        String csp = mvc.perform(get("/api/hello")).andReturn().getResponse().getHeader("Content-Security-Policy");
        assertThat(csp).isNotNull().doesNotContain("ui-avatars");
    }

    private static byte[] bytesDe(String archivo) throws IOException {
        return Files.readAllBytes(CARPETA_AVATARES.resolve(archivo));
    }

    private static boolean mencionaUiAvatars(Path ruta) {
        try {
            return Files.readString(ruta, StandardCharsets.UTF_8).contains("ui-avatars");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
