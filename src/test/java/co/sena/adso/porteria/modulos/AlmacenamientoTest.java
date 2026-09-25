package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.exception.DatoInvalidoException;
import co.sena.adso.porteria.service.FotoService;
import co.sena.adso.porteria.service.RespaldoService;
import co.sena.adso.porteria.soporte.Perfil;
import co.sena.adso.porteria.soporte.PruebaIntegracion;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.ResultActions;

// Portería 2: tests/modulos/test_almacenamiento.py
class AlmacenamientoTest extends PruebaIntegracion {

    @Value("${app.fotos.carpeta}")
    private String carpetaFotos;

    @TempDir
    Path tmp;

    @ParameterizedTest(name = "{0}")
    @MethodSource("permitidas")
    void permitidas(String nombre, byte[] contenido) throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        subir(aprendiz, nombre, contenido).andExpect(status().isOk());
    }

    static Stream<Arguments> permitidas() throws IOException {
        return Stream.of(
                Arguments.of("foto.jpg", imagen(600, 600, "jpg")),
                Arguments.of("FOTO.JPEG", imagen(600, 600, "jpg")),
                Arguments.of("a.png", imagen(600, 600, "png")),
                // WEBP de 1x1 px sin pérdida: ImageIO no sabe escribir este formato
                Arguments.of("b.webp", Base64.getDecoder().decode("UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA==")));
    }

    // Spring decide por el contenido real del archivo, no por la extensión del nombre
    @ParameterizedTest(name = "{0}")
    @MethodSource("rechazadas")
    void rechazadas(String nombre, byte[] contenido) throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        subir(aprendiz, nombre, contenido).andExpect(status().isBadRequest());
        assertThat(archivosEn(Path.of(carpetaFotos))).isEmpty();
    }

    static Stream<Arguments> rechazadas() {
        return Stream.of(
                Arguments.of("x.svg", texto("<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>")),
                Arguments.of("x.php", texto("<?php system($_GET[\"c\"]); ?>")),
                Arguments.of("x.exe", new byte[] {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0}),
                Arguments.of("sin_extension", texto("no soy una imagen")),
                Arguments.of(Named.of("vacío", ""), texto("no soy una imagen")),
                Arguments.of(Named.of("None", null), texto("no soy una imagen")));
    }

    // Spring no usa nombre fijo sino que borra la anterior: lo que importa es no acumular huérfanas
    @Test
    void nombreFijoPorUsuario() throws Exception {
        Sesion aprendiz = entrarComo(Perfil.APRENDIZ);
        subir(aprendiz, "foto.jpg", imagen(600, 600, "jpg")).andExpect(status().isOk());
        subir(aprendiz, "foto.jpg", imagen(600, 600, "jpg")).andExpect(status().isOk());

        List<Path> archivos = archivosEn(Path.of(carpetaFotos));
        assertThat(archivos).hasSize(1);
        assertThat(archivos.get(0).getFileName().toString()).isEqualTo(fotoGuardada(aprendiz));
    }

    @Test
    void reescalaComprimeYConvierte() throws Exception {
        byte[] origen = imagen(2400, 2400, "jpg");
        new FotoService(tmp.toString()).guardar(usuario(), archivo("salida.jpg", origen));
        Path destino = unicoArchivo(tmp);

        assertThat(Files.size(destino)).isLessThan(origen.length / 5);
        BufferedImage guardada = ImageIO.read(destino.toFile());
        assertThat(Math.max(guardada.getWidth(), guardada.getHeight())).isLessThanOrEqualTo(512);
        assertThat(formatoDe(destino)).isEqualTo("JPEG");
    }

    @Test
    void eliminaLosMetadatosExif() throws Exception {
        byte[] conExif = conExif(imagen(800, 800, "jpg"), "FabricanteDePrueba");
        assertThat(contiene(conExif, "FabricanteDePrueba")).as("la imagen de partida debe traer EXIF").isTrue();

        new FotoService(tmp.toString()).guardar(usuario(), archivo("con_exif.jpg", conExif));
        byte[] limpia = Files.readAllBytes(unicoArchivo(tmp));
        assertThat(contiene(limpia, "Exif")).isFalse();
        assertThat(contiene(limpia, "FabricanteDePrueba")).isFalse();
    }

    @Test
    void aplanaLaTransparenciaSobreBlanco() throws Exception {
        BufferedImage transparente = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        new FotoService(tmp.toString()).guardar(usuario(), archivo("transparente.png", codificar(transparente, "png")));

        BufferedImage guardada = ImageIO.read(unicoArchivo(tmp).toFile());
        assertThat(new Color(guardada.getRGB(5, 5))).isEqualTo(Color.WHITE);
    }

    @Test
    void rechazaLoQueNoEsUnaImagen() {
        FotoService fotos = new FotoService(tmp.toString());
        assertThatThrownBy(() -> fotos.guardar(usuario(), archivo("x.jpg", texto("<?php system($_GET[\"c\"]); ?>"))))
                .isInstanceOf(DatoInvalidoException.class);
        assertThat(archivosEn(tmp)).isEmpty();
    }

    @Test
    void creaLaCarpetaSiNoExiste() throws Exception {
        Path carpeta = tmp.resolve("sub").resolve("carpeta");
        String nombre = new FotoService(carpeta.toString()).guardar(usuario(), archivo("nueva.jpg", imagen(600, 600, "jpg")));
        assertThat(carpeta.resolve(nombre)).isRegularFile();
    }

    @Test
    void laFotoSeGuardaComprimidaYConNombreFijo() throws Exception {
        Sesion ana = entrarComo(Perfil.APRENDIZ, u -> u.setPerfilCompleto(false));
        subir(ana, "enorme.jpg", imagen(2400, 2400, "jpg")).andExpect(status().isOk());

        String guardada = fotoGuardada(ana);
        assertThat(Path.of(carpetaFotos).resolve(guardada)).isRegularFile();
        assertThat(temporales()).isEmpty();
    }

    // En el servidor las fotos van en un volumen aparte y un move entre sistemas de archivos falla
    @Test
    void laSubidaFuncionaConLasFotosEnOtroVolumen() throws Exception {
        Sesion ana = entrarComo(Perfil.APRENDIZ, u -> u.setPerfilCompleto(false));
        Path carpeta = Path.of(carpetaFotos).toAbsolutePath().normalize();
        byte[] foto = imagen(600, 600, "jpg");

        try (MockedStatic<Files> archivos = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            archivos.when(() -> Files.move(any(Path.class), any(Path.class), any(CopyOption[].class)))
                    .thenAnswer(llamada -> {
                        Path origen = llamada.<Path>getArgument(0).toAbsolutePath().normalize();
                        if (!carpeta.equals(origen.getParent())) {
                            throw new IOException("Invalid cross-device link");
                        }
                        return llamada.callRealMethod();
                    });
            subir(ana, "foto.jpg", foto).andExpect(status().isOk());
            archivos.verify(() -> Files.move(any(Path.class), any(Path.class), any(CopyOption[].class)));
        }
        assertThat(carpeta.resolve(fotoGuardada(ana))).isRegularFile();
        assertThat(temporales()).isEmpty();
    }

    @Test
    void unArchivoInvalidoNoBorraLaFotoAnterior() throws Exception {
        Sesion ana = entrarComo(Perfil.APRENDIZ);
        String nombre = "user_" + ana.usuario().getId() + ".jpg";
        Path anterior = Files.createDirectories(Path.of(carpetaFotos)).resolve(nombre);
        Files.write(anterior, texto("foto anterior"));
        jdbc.update("UPDATE usuarios SET foto = ? WHERE id = ?", nombre, ana.usuario().getId());

        subir(ana, "x.jpg", texto("no soy una imagen")).andExpect(status().isBadRequest());
        assertThat(anterior).isRegularFile();
        assertThat(Files.readAllBytes(anterior)).isEqualTo(texto("foto anterior"));
        assertThat(temporales()).isEmpty();
    }

    private ResultActions subir(Sesion sesion, String nombre, byte[] contenido) throws Exception {
        return mvc.perform(con(sesion, multipart("/api/perfil/foto").file(archivo(nombre, contenido))));
    }

    private String fotoGuardada(Sesion sesion) {
        return jdbc.queryForObject("SELECT foto FROM usuarios WHERE id = ?", String.class, sesion.usuario().getId());
    }

    private List<Path> temporales() {
        return archivosEn(Path.of(carpetaFotos)).stream()
                .filter(r -> r.getFileName().toString().startsWith(".tmp_")).toList();
    }

    private static List<Path> archivosEn(Path carpeta) {
        if (!Files.isDirectory(carpeta)) {
            return List.of();
        }
        try (Stream<Path> rutas = Files.list(carpeta)) {
            return rutas.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Path unicoArchivo(Path carpeta) {
        List<Path> archivos = archivosEn(carpeta);
        assertThat(archivos).hasSize(1);
        return archivos.get(0);
    }

    private static Usuario usuario() {
        Usuario u = mock(Usuario.class);
        when(u.getId()).thenReturn(7L);
        return u;
    }

    private static MockMultipartFile archivo(String nombre, byte[] contenido) {
        return new MockMultipartFile("foto", nombre, "application/octet-stream", contenido);
    }

    private static byte[] texto(String contenido) {
        return contenido.getBytes(StandardCharsets.UTF_8);
    }

    // Cara dibujada: sirve para el procesado, que no necesita reconocer a nadie
    private static byte[] imagen(int ancho, int alto, String formato) throws IOException {
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(235, 225, 215));
        g.fillRect(0, 0, ancho, alto);
        int cx = ancho / 2;
        int cy = alto / 2;
        int r = Math.min(ancho, alto) / 4;
        g.setColor(new Color(228, 205, 185));
        g.fillOval(cx - r, cy - r * 5 / 4, 2 * r, r * 5 / 2);
        g.setColor(new Color(40, 30, 25));
        g.fillOval(cx - r / 2 - r / 4, cy - r / 2, r / 2, r / 4);
        g.fillOval(cx + r / 2 - r / 4, cy - r / 2, r / 2, r / 4);
        g.setColor(new Color(120, 70, 60));
        g.drawArc(cx - r / 2, cy + r / 3, r, r * 2 / 3, 180, 180);
        g.dispose();
        return codificar(img, formato);
    }

    private static byte[] codificar(BufferedImage img, String formato) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        if (!"jpg".equals(formato)) {
            ImageIO.write(img, formato, salida);
            return salida.toByteArray();
        }
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam parametros = escritor.getDefaultWriteParam();
        parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        parametros.setCompressionQuality(0.95f);
        try (MemoryCacheImageOutputStream flujo = new MemoryCacheImageOutputStream(salida)) {
            escritor.setOutput(flujo);
            escritor.write(null, new IIOImage(img, null, null), parametros);
        } finally {
            escritor.dispose();
        }
        return salida.toByteArray();
    }

    // Inserta tras el SOI un segmento APP1 Exif con la etiqueta Make (271), como las fotos de celular
    private static byte[] conExif(byte[] jpeg, String fabricante) {
        byte[] valor = (fabricante + "\0").getBytes(StandardCharsets.US_ASCII);
        ByteArrayOutputStream tiff = new ByteArrayOutputStream();
        tiff.writeBytes(new byte[] {'M', 'M', 0, 42, 0, 0, 0, 8, 0, 1});
        tiff.writeBytes(new byte[] {0x01, 0x0F, 0, 2, 0, 0, 0, (byte) valor.length, 0, 0, 0, 26});
        tiff.writeBytes(new byte[] {0, 0, 0, 0});
        tiff.writeBytes(valor);
        byte[] cuerpo = tiff.toByteArray();
        int largo = 2 + 6 + cuerpo.length;

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        salida.write(0xFF);
        salida.write(0xD8);
        salida.writeBytes(new byte[] {(byte) 0xFF, (byte) 0xE1, (byte) (largo >> 8), (byte) largo});
        salida.writeBytes(new byte[] {'E', 'x', 'i', 'f', 0, 0});
        salida.writeBytes(cuerpo);
        salida.write(jpeg, 2, jpeg.length - 2);
        return salida.toByteArray();
    }

    private static boolean contiene(byte[] datos, String buscado) {
        return new String(datos, StandardCharsets.ISO_8859_1).contains(buscado);
    }

    private static String formatoDe(Path ruta) throws IOException {
        try (var flujo = ImageIO.createImageInputStream(ruta.toFile())) {
            return ImageIO.getImageReaders(flujo).next().getFormatName().toUpperCase();
        }
    }

    @Autowired
    private RespaldoService respaldoService;

    @Autowired
    private Clock relojRespaldo;

    private Path archivoDelMesPasado() throws IOException {
        Path ruta = respaldoService.carpeta().resolve(RespaldoService.nombreArchivo(LocalDate.now(relojRespaldo)));
        Files.deleteIfExists(ruta);
        return ruta;
    }

    private void accesoDelMesPasado(Long referencia, String tipoReferencia) {
        jdbc.update("INSERT INTO accesos (punto_id, referencia_id, tipo_referencia, tipo, fecha) VALUES (1, ?, ?, 'Entrada', ?)",
                referencia, tipoReferencia, LocalDate.now(relojRespaldo).withDayOfMonth(1).minusDays(5).atTime(8, 0));
    }

    // referencia_id es polimórfico: sin filtrar por tipo, el visitante 7 se archivaba con la cédula del usuario 7
    @Test
    void noMezclaAccesosDeDistintasEntidades() throws Exception {
        Usuario usuario = crearUsuario("ana@sena.edu.co", "12345");
        accesoDelMesPasado(usuario.getId(), "Usuario");
        accesoDelMesPasado(usuario.getId(), "Visitante");
        Path ruta = archivoDelMesPasado();

        respaldoService.respaldarMesAnterior();

        assertThat(ruta).as("el respaldo debió generar un archivo").isRegularFile();
        try (ReadableWorkbook libro = new ReadableWorkbook(ruta.toFile())) {
            assertThat(libro.getSheets().map(Sheet::getName)).as("visitantes y vehículos también se archivan")
                    .contains("Accesos No Usuarios");
            List<Row> filas = libro.findSheet("Historial Accesos").orElseThrow().read();
            assertThat(filas).as("solo el acceso del usuario va en esa hoja").hasSize(2);
        }
    }

    @Test
    void soloBorraSiSePideAProposito() throws Exception {
        Usuario usuario = crearUsuario("beto@sena.edu.co", "543210");
        accesoDelMesPasado(usuario.getId(), "Usuario");
        Path ruta = archivoDelMesPasado();
        ReflectionTestUtils.setField(respaldoService, "purgar", true);
        try {
            respaldoService.respaldarMesAnterior();
        } finally {
            ReflectionTestUtils.setField(respaldoService, "purgar", false);
        }
        assertThat(ruta).isRegularFile();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM accesos WHERE fecha < ?", Long.class,
                LocalDate.now(relojRespaldo).withDayOfMonth(1).atStartOfDay())).isZero();
    }
}
