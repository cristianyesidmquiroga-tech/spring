package co.sena.adso.porteria.service;

import co.sena.adso.porteria.entity.Acceso;
import co.sena.adso.porteria.entity.AsistenciaClase;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.exception.ResourceNotFoundException;
import co.sena.adso.porteria.repository.AccesoRepository;
import co.sena.adso.porteria.repository.AsistenciaClaseRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Exporta el mes anterior a Excel; solo borra si se pide a propósito y el archivo se pudo releer
@Service
public class RespaldoService {

    private static final Logger log = LoggerFactory.getLogger(RespaldoService.class);
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^Respaldo_Sistema_\\d{4}-\\d{2}\\.xlsx$");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AccesoRepository accesoRepository;
    private final AsistenciaClaseRepository asistenciaRepository;
    private final AuditoriaService auditoriaService;
    private final CorreoService correoService;
    private final PlantillasCorreo plantillas;
    private final Clock reloj;
    private final Path carpeta;
    private final String correoAdmin;
    private boolean purgar;

    public RespaldoService(AccesoRepository accesoRepository, AsistenciaClaseRepository asistenciaRepository,
                           AuditoriaService auditoriaService, CorreoService correoService, PlantillasCorreo plantillas,
                           Clock reloj, @Value("${app.respaldos.carpeta:datos/respaldos}") String carpeta,
                           @Value("${app.respaldos.purgar:false}") boolean purgar,
                           @Value("${app.admin.email:}") String correoAdmin) {
        this.accesoRepository = accesoRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.auditoriaService = auditoriaService;
        this.correoService = correoService;
        this.plantillas = plantillas;
        this.reloj = reloj;
        this.carpeta = Path.of(carpeta).toAbsolutePath().normalize();
        this.purgar = purgar;
        this.correoAdmin = correoAdmin;
    }

    @Scheduled(cron = "10 0 0 1 * *", zone = "America/Bogota")
    public void respaldoProgramado() {
        respaldarMesAnterior();
    }

    public static String nombreArchivo(LocalDate hoy) {
        return "Respaldo_Sistema_" + hoy.withDayOfMonth(1).minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM")) + ".xlsx";
    }

    public Path carpeta() {
        return carpeta;
    }

    @Transactional
    public void respaldarMesAnterior() {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDateTime fin = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDateTime inicio = fin.minusMonths(1);
        String mes = inicio.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String nombre = nombreArchivo(hoy);

        // referencia_id es polimórfico: los accesos de usuarios se unen solo con tipo_referencia = Usuario
        List<Object[]> deUsuarios = accesoRepository.accesosDePersonas(inicio, fin, "", "");
        List<Acceso> otros = accesoRepository.findByTipoReferenciaNotAndFechaGreaterThanEqualAndFechaLessThanOrderByFechaAsc(
                "Usuario", inicio, fin);
        List<AsistenciaClase> asistencias = asistenciaRepository.enRango(inicio, fin);
        if (deUsuarios.isEmpty() && otros.isEmpty() && asistencias.isEmpty()) {
            log.info("No hay datos del mes {} para respaldar", mes);
            return;
        }

        Path ruta = carpeta.resolve(nombre);
        try {
            Files.createDirectories(carpeta);
            escribir(ruta, deUsuarios, otros, asistencias);
        } catch (IOException | RuntimeException e) {
            fallo(mes, ruta, "no se pudo escribir el archivo (" + e.getClass().getSimpleName() + ")");
            return;
        }
        String problema = verificar(ruta);
        if (problema != null) {
            fallo(mes, ruta, problema);
            return;
        }
        if (!purgar) {
            auditoriaService.registrarSistema("respaldos_mensuales", "Respaldo mensual generado",
                    "Archivo: " + nombre + ". No se borró ningún dato.", LocalDateTime.now(reloj));
            return;
        }
        List<Long> accesos = new ArrayList<>();
        deUsuarios.forEach(f -> accesos.add(((Acceso) f[0]).getId()));
        otros.forEach(a -> accesos.add(a.getId()));
        accesoRepository.deleteAllByIdInBatch(accesos);
        asistenciaRepository.deleteAllByIdInBatch(asistencias.stream().map(AsistenciaClase::getId).toList());
        log.warn("Respaldo {} generado: se borraron {} accesos y {} asistencias", nombre, accesos.size(), asistencias.size());
        auditoriaService.registrarSistema("respaldos_mensuales", "Respaldo mensual generado CON BORRADO",
                "Archivo: " + nombre + ". Borrados " + accesos.size() + " accesos y " + asistencias.size() + " asistencias.",
                LocalDateTime.now(reloj));
    }

    // Solo si el respaldo borra datos: faltando 15 o menos de 3 días se avisa al admin
    public String aviso() {
        if (!purgar) {
            return null;
        }
        LocalDate hoy = LocalDate.now(reloj);
        long dias = ChronoUnit.DAYS.between(hoy, hoy.withDayOfMonth(1).plusMonths(1));
        if (dias <= 3) {
            return "ATENCIÓN: faltan " + dias + " días para la limpieza automática de la base de datos.";
        }
        return dias == 15 ? "Aviso (15 días): el sistema hará un respaldo y limpieza de datos antiguos el primer día del mes." : null;
    }

    public List<String> listar() {
        if (!Files.isDirectory(carpeta)) {
            return List.of();
        }
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos.map(p -> p.getFileName().toString()).filter(n -> NOMBRE_VALIDO.matcher(n).matches())
                    .sorted((a, b) -> b.compareTo(a)).toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    // Solo nombres del patrón y dentro de la carpeta: nada de ../ hacia el resto del servidor
    public Resource archivo(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            throw new ResourceNotFoundException("un respaldo", 0L);
        }
        Path ruta = carpeta.resolve(nombre).normalize();
        if (!ruta.startsWith(carpeta) || !Files.isRegularFile(ruta)) {
            throw new ResourceNotFoundException("un respaldo", 0L);
        }
        return new FileSystemResource(ruta);
    }

    private void escribir(Path ruta, List<Object[]> deUsuarios, List<Acceso> otros, List<AsistenciaClase> asistencias)
            throws IOException {
        try (OutputStream salida = Files.newOutputStream(ruta)) {
            Workbook libro = new Workbook(salida, "Porteria SENA", "1.0");
            Worksheet hoja = libro.newWorksheet("Historial Accesos");
            fila(hoja, 0, "ID", "Documento", "Nombre", "Cargo", "Ficha", "Tipo", "Equipos", "Fecha");
            int r = 1;
            for (Object[] f : deUsuarios) {
                Acceso a = (Acceso) f[0];
                Usuario u = (Usuario) f[1];
                fila(hoja, r++, a.getId(), u.getDocumento(), u.getNombre(), u.getCargo() != null ? u.getCargo() : u.getRol().getNombre(),
                        u.numeroFicha() != null ? u.numeroFicha() : "N/A", a.getTipo(),
                        a.getEquiposIds() != null && !a.getEquiposIds().isBlank() ? a.getEquiposIds() : "Ninguno", a.getFecha().format(FECHA));
            }
            Worksheet noUsuarios = libro.newWorksheet("Accesos No Usuarios");
            fila(noUsuarios, 0, "ID", "Tipo de entidad", "Referencia", "Tipo", "Fecha");
            r = 1;
            for (Acceso a : otros) {
                fila(noUsuarios, r++, a.getId(), a.getTipoReferencia(), a.getReferenciaId(), a.getTipo(), a.getFecha().format(FECHA));
            }
            Worksheet clases = libro.newWorksheet("Asistencias Clases");
            fila(clases, 0, "ID", "Ficha", "Instructor", "Aprendiz Documento", "Aprendiz Nombre", "Presente", "Fecha");
            r = 1;
            for (AsistenciaClase a : asistencias) {
                fila(clases, r++, a.getId(), a.getFicha(), a.getInstructor() != null ? a.getInstructor().getNombre() : "Desconocido",
                        a.getAprendiz().getDocumento() != null ? a.getAprendiz().getDocumento() : "N/A",
                        a.getAprendiz().getNombre(), a.isPresente() ? "SI" : "NO", a.getFecha().format(FECHA));
            }
            libro.finish();
        }
    }

    private static void fila(Worksheet hoja, int fila, Object... valores) {
        for (int c = 0; c < valores.length; c++) {
            Object v = valores[c];
            if (v instanceof Number n) {
                hoja.value(fila, c, n);
            } else {
                hoja.value(fila, c, v == null ? "" : v.toString());
            }
        }
    }

    // Antes de borrar se comprueba que el archivo quedó escrito y se puede volver a abrir
    private static String verificar(Path ruta) {
        try {
            if (!Files.exists(ruta)) {
                return "el archivo no existe después de guardarlo";
            }
            if (Files.size(ruta) == 0) {
                return "el archivo quedó vacío (0 bytes)";
            }
            try (ReadableWorkbook libro = new ReadableWorkbook(ruta.toFile())) {
                return libro.getSheets().findAny().isPresent() ? null : "el archivo se abre pero no tiene ninguna hoja";
            }
        } catch (IOException | RuntimeException e) {
            return "el archivo no se puede volver a abrir (" + e.getClass().getSimpleName() + ")";
        }
    }

    // Un respaldo que falla en silencio todos los meses es igual a no tener respaldo
    private void fallo(String mes, Path ruta, String problema) {
        log.error("Respaldo mensual abortado: {}", problema);
        auditoriaService.registrarSistema("respaldos_mensuales", "Respaldo mensual ABORTADO: el archivo no superó la verificación",
                "No se borró ningún dato. Archivo: " + ruta.getFileName() + ". Problema: " + problema + ".", LocalDateTime.now(reloj));
        if (!correoAdmin.isBlank()) {
            correoService.enviar(correoAdmin, "[Sistema de Acceso] Falló el respaldo mensual (" + mes + ")",
                    plantillas.falloRespaldo(mes, problema));
        }
    }
}
