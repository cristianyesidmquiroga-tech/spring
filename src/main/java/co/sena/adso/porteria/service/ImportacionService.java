package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.ImportacionResponseDTO;
import co.sena.adso.porteria.entity.Rol;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.exception.DatoInvalidoException;
import co.sena.adso.porteria.repository.FichaRepository;
import co.sena.adso.porteria.repository.RolRepository;
import co.sena.adso.porteria.repository.UsuarioRepository;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.CellType;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

// Quien prepara la hoja no es quien la sube: el archivo nunca decide quién es administrador
@Service
public class ImportacionService {

    private static final Set<String> ROLES_IMPORTABLES = Set.of("usuario", "trabajador");

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final FichaRepository fichaRepository;
    private final DocumentoService documentoService;
    private final AuthService authService;
    private final AuditoriaService auditoriaService;
    private final CorreoService correoService;
    private final PlantillasCorreo plantillas;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom azar = new SecureRandom();

    public ImportacionService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                              FichaRepository fichaRepository, DocumentoService documentoService, AuthService authService,
                              AuditoriaService auditoriaService, CorreoService correoService, PlantillasCorreo plantillas,
                              PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.fichaRepository = fichaRepository;
        this.documentoService = documentoService;
        this.authService = authService;
        this.auditoriaService = auditoriaService;
        this.correoService = correoService;
        this.plantillas = plantillas;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ImportacionResponseDTO importar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new DatoInvalidoException("No se subió ningún archivo");
        }
        List<Row> filas;
        try (InputStream entrada = archivo.getInputStream(); ReadableWorkbook libro = new ReadableWorkbook(entrada)) {
            filas = libro.getFirstSheet().read();
        } catch (IOException | RuntimeException e) {
            throw new DatoInvalidoException("El archivo no es un Excel válido (.xlsx)");
        }
        Map<String, Integer> columnas = new HashMap<>();
        if (!filas.isEmpty()) {
            for (Cell celda : filas.get(0)) {
                String titulo = texto(celda);
                if (titulo != null) {
                    columnas.put(titulo, celda.getColumnIndex());
                }
            }
        }
        for (String obligatoria : List.of("Nombre", "Correo")) {
            if (!columnas.containsKey(obligatoria)) {
                throw new DatoInvalidoException("Falta la columna '" + obligatoria + "' en el Excel");
            }
        }

        Usuario admin = authService.usuarioActual();
        int creados = 0;
        int omitidos = 0;
        List<String> errores = new ArrayList<>();
        // La columna documento es única: dos filas iguales del mismo archivo no pueden entrar ambas
        Set<String> documentosDelLote = new HashSet<>();
        for (int i = 1; i < filas.size(); i++) {
            Row fila = filas.get(i);
            int numero = fila.getRowNum();
            String nombre = Texto.opcional(celda(fila, columnas, "Nombre"));
            String correo = celda(fila, columnas, "Correo");
            // Las filas que no se van a crear no generan avisos (una fila vacía del final no es un error)
            if (nombre == null || correo == null) {
                omitidos++;
                continue;
            }
            correo = correo.toLowerCase();
            if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
                omitidos++;
                continue;
            }

            String tipo = celda(fila, columnas, "Tipo Documento");
            String documento = celda(fila, columnas, "Documento");
            if (documento != null) {
                try {
                    if (tipo == null) {
                        if (!documentoService.normalizar(documento).chars().allMatch(Character::isDigit)) {
                            throw new DatoInvalidoException("un número con letras solo es válido como pasaporte y hay que declararlo");
                        }
                        tipo = documentoService.tipoProbable(documento);
                    }
                    documento = documentoService.validar(tipo.toUpperCase(), documento);
                } catch (DatoInvalidoException e) {
                    errores.add("Fila " + numero + ": documento no válido (" + e.getMessage() + "). Se importó sin documento.");
                    documento = null;
                }
            }
            if (documento != null && (documentosDelLote.contains(documento) || usuarioRepository.existsByDocumento(documento))) {
                errores.add("Fila " + numero + ": el documento ya está registrado. Se importó sin documento.");
                documento = null;
            }

            String cargoHoja = celda(fila, columnas, "Cargo");
            String cargo = cargoHoja != null && Usuario.CARGOS_VALIDOS.contains(cargoHoja) ? cargoHoja : "Aprendiz";
            if (cargoHoja != null && !cargo.equals(cargoHoja)) {
                errores.add("Fila " + numero + ": cargo '" + cargoHoja + "' no válido, se asignó Aprendiz.");
            }
            String rolHoja = celda(fila, columnas, "Rol");
            String rol = rolHoja != null && ROLES_IMPORTABLES.contains(rolHoja.toLowerCase()) ? rolHoja.toLowerCase() : "usuario";
            if (rolHoja != null && !rol.equals(rolHoja.toLowerCase())) {
                errores.add("Fila " + numero + ": rol '" + rolHoja + "' no permitido en importación, se asignó Usuario.");
            }

            // Una contraseña fija en el código abriría toda cuenta importada antes de que su dueño la use
            String clave = celda(fila, columnas, "Contraseña");
            if (clave == null) {
                byte[] bytes = new byte[12];
                azar.nextBytes(bytes);
                clave = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            }
            Usuario nuevo = new Usuario(nombre, correo, passwordEncoder.encode(clave),
                    rolRepository.findByNombre("trabajador".equals(rol) ? Rol.TRABAJADOR : Rol.USUARIO).orElseThrow(), cargo);
            nuevo.setTipoDocumento(tipo == null ? DocumentoService.TIPO_POR_DEFECTO : tipo.toUpperCase());
            nuevo.setDocumento(documento);
            String ficha = celda(fila, columnas, "Ficha");
            nuevo.setFicha(ficha);
            nuevo.setPrograma(Texto.opcional(celda(fila, columnas, "Programa")));
            nuevo.setHorario(celda(fila, columnas, "Horario"));
            nuevo.asignarFicha(ficha == null ? null : fichaRepository.findByNumero(ficha).orElse(null));
            nuevo.setCorreoVerificado(true);
            nuevo.setDebeCambiarContrasena(true);
            usuarioRepository.save(nuevo);
            if (documento != null) {
                documentosDelLote.add(documento);
            }
            creados++;
            // La cuenta ya está creada: un correo que falla no la descuenta del lote
            try {
                correoService.enviar(correo, "Bienvenido al Sistema de Acceso - SENA", plantillas.bienvenida(nombre, correo, clave));
            } catch (RuntimeException e) {
                errores.add("Fila " + numero + ": usuario creado, pero no se pudo enviar el correo de bienvenida.");
            }
        }

        auditoriaService.registrar(admin, "usuarios", 0L, "Importación masiva de usuarios", admin.getNombre(),
                "Carga de usuarios desde archivo Excel", "Archivo: " + archivo.getOriginalFilename() + ". Creados: " + creados
                        + ", omitidos: " + omitidos + ", avisos: " + errores.size() + ".");
        String mensaje = "Importación finalizada. Creados: " + creados + ", Omitidos: " + omitidos + "."
                + (errores.isEmpty() ? "" : " Errores: " + errores.size());
        return new ImportacionResponseDTO(mensaje, new ImportacionResponseDTO.Detalles(creados, omitidos, errores));
    }

    private static String celda(Row fila, Map<String, Integer> columnas, String nombre) {
        Integer indice = columnas.get(nombre);
        return indice == null || !fila.hasCell(indice) ? null : texto(fila.getCell(indice));
    }

    // Un número de Excel sale sin ".0": el documento guardado debe ser el mismo que se teclea en portería
    private static String texto(Cell celda) {
        if (celda == null || celda.getType() == CellType.EMPTY) {
            return null;
        }
        String valor = celda.getType() == CellType.NUMBER ? celda.asNumber().stripTrailingZeros().toPlainString() : celda.getText();
        valor = valor == null ? "" : valor.trim();
        return valor.isEmpty() ? null : valor;
    }
}
