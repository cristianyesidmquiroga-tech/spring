package co.sena.adso.porteria.service;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.exception.DatoInvalidoException;
import co.sena.adso.porteria.exception.ResourceNotFoundException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Las fotos viven fuera de cualquier carpeta pública y solo se entregan por la API,
 * que revisa quién las pide. Se re-codifican a JPEG, lo que además elimina los
 * metadatos EXIF (en fotos de celular traen la ubicación GPS).
 */
@Service
public class FotoService {

    private static final Logger log = LoggerFactory.getLogger(FotoService.class);
    private static final Set<String> FORMATOS_PERMITIDOS = Set.of("jpeg", "jpg", "png", "bmp");
    private static final int LADO_MAXIMO = 800;
    private static final int LADO_MAXIMO_ENTRADA = 6000;

    private final Path carpeta;
    private final SecureRandom aleatorio = new SecureRandom();

    public FotoService(@Value("${app.fotos.carpeta}") String carpeta) {
        this.carpeta = Path.of(carpeta).toAbsolutePath().normalize();
    }

    /** Guarda la foto nueva y borra la anterior. Devuelve el nombre del archivo guardado. */
    public String guardar(Usuario usuario, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new DatoInvalidoException("Selecciona una imagen");
        }
        BufferedImage imagen = leer(archivo);
        String nombre = "usuario_" + usuario.getId() + "_" + sufijoAleatorio() + ".jpg";
        try {
            Files.createDirectories(carpeta);
            // Primero a un temporal: si algo falla, la foto anterior sigue intacta
            Path temporal = Files.createTempFile(carpeta, ".tmp_", ".jpg");
            try {
                ImageIO.write(ajustar(imagen), "jpg", temporal.toFile());
                Files.move(temporal, carpeta.resolve(nombre), StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temporal);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la foto", e);
        }
        borrar(usuario.getFoto());
        return nombre;
    }

    public Resource obtener(Usuario usuario) {
        Path ruta = resolver(usuario.getFoto());
        if (ruta == null || !Files.isRegularFile(ruta)) {
            throw new ResourceNotFoundException("una foto para el usuario", usuario.getId());
        }
        return new PathResource(ruta);
    }

    public void borrar(String nombre) {
        Path ruta = resolver(nombre);
        if (ruta == null) {
            return;
        }
        try {
            Files.deleteIfExists(ruta);
        } catch (IOException e) {
            log.warn("No se pudo borrar la foto {}", nombre);
        }
    }

    // Solo se aceptan nombres que queden dentro de la carpeta de fotos (evita path traversal)
    private Path resolver(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        Path ruta = carpeta.resolve(nombre).normalize();
        return ruta.startsWith(carpeta) ? ruta : null;
    }

    // Se leen las dimensiones antes de decodificar: una imagen de 50.000 px de lado agotaría la memoria
    private BufferedImage leer(MultipartFile archivo) {
        try (InputStream entrada = archivo.getInputStream();
             ImageInputStream flujo = ImageIO.createImageInputStream(entrada)) {
            Iterator<ImageReader> lectores = flujo == null ? null : ImageIO.getImageReaders(flujo);
            if (lectores == null || !lectores.hasNext()) {
                throw new DatoInvalidoException("El archivo no es una imagen válida");
            }
            ImageReader lector = lectores.next();
            // Se valida el formato real del archivo, no la cabecera que manda el cliente
            if (!FORMATOS_PERMITIDOS.contains(lector.getFormatName().toLowerCase())) {
                lector.dispose();
                throw new DatoInvalidoException("Solo se permiten imágenes JPG, PNG o BMP");
            }
            try {
                lector.setInput(flujo, true, true);
                if (lector.getWidth(0) > LADO_MAXIMO_ENTRADA || lector.getHeight(0) > LADO_MAXIMO_ENTRADA) {
                    throw new DatoInvalidoException("La imagen es demasiado grande, máximo "
                            + LADO_MAXIMO_ENTRADA + " px por lado");
                }
                return lector.read(0);
            } finally {
                lector.dispose();
            }
        } catch (IOException e) {
            throw new DatoInvalidoException("No se pudo leer la imagen");
        }
    }

    // Reescala al lado máximo y aplana la transparencia sobre blanco
    private BufferedImage ajustar(BufferedImage original) {
        double escala = Math.min(1.0, (double) LADO_MAXIMO / Math.max(original.getWidth(), original.getHeight()));
        int ancho = (int) Math.round(original.getWidth() * escala);
        int alto = (int) Math.round(original.getHeight() * escala);
        BufferedImage destino = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, ancho, alto);
        g.drawImage(original, 0, 0, ancho, alto, null);
        g.dispose();
        return destino;
    }

    private String sufijoAleatorio() {
        byte[] bytes = new byte[8];
        aleatorio.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
