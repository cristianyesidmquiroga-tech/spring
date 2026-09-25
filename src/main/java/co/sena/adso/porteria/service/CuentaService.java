package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.MensajeResponseDTO;
import co.sena.adso.porteria.dto.PermisoRecuperacionResponseDTO;
import co.sena.adso.porteria.dto.RecuperacionCambioRequestDTO;
import co.sena.adso.porteria.dto.RegistroRequestDTO;
import co.sena.adso.porteria.dto.UsuarioSesionDTO;
import co.sena.adso.porteria.entity.Rol;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.exception.ConflictoException;
import co.sena.adso.porteria.exception.DatoInvalidoException;
import co.sena.adso.porteria.repository.FichaRepository;
import co.sena.adso.porteria.repository.RolRepository;
import co.sena.adso.porteria.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Registro público, verificación del correo y recuperación de contraseña
@Service
public class CuentaService {

    private static final Logger log = LoggerFactory.getLogger(CuentaService.class);
    private static final int MINUTOS_VIGENCIA = 15;
    private static final int MAX_INTENTOS_CODIGO = 5;
    private static final int CODIGOS_POR_HORA_POR_CORREO = 3;
    // Confirmar que un correo o una cédula ya existen permitiría enumerar las cuentas del centro
    private static final String MENSAJE_NEUTRO_REGISTRO =
            "Si los datos son correctos, recibirás un correo con las instrucciones para continuar.";
    private static final String MENSAJE_NEUTRO_RECUPERACION =
            "Si el correo está registrado, enviamos un código de recuperación. Revisa tu bandeja de entrada.";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final FichaRepository fichaRepository;
    private final DocumentoService documentoService;
    private final AuthService authService;
    private final CaptchaService captchaService;
    private final CorreoService correoService;
    private final PlantillasCorreo plantillas;
    private final PasswordEncoder passwordEncoder;
    private final Clock reloj;
    private final List<String> dominios;
    private final SecureRandom azar = new SecureRandom();
    private final Map<String, Deque<Long>> codigosPorCorreo = new ConcurrentHashMap<>();

    public CuentaService(UsuarioRepository usuarioRepository, RolRepository rolRepository, FichaRepository fichaRepository,
                         DocumentoService documentoService, AuthService authService, CaptchaService captchaService,
                         CorreoService correoService, PlantillasCorreo plantillas, PasswordEncoder passwordEncoder,
                         Clock reloj, @Value("${app.registro.dominios:}") String dominios) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.fichaRepository = fichaRepository;
        this.documentoService = documentoService;
        this.authService = authService;
        this.captchaService = captchaService;
        this.correoService = correoService;
        this.plantillas = plantillas;
        this.passwordEncoder = passwordEncoder;
        this.reloj = reloj;
        this.dominios = Arrays.stream(dominios.split(",")).map(String::trim).map(String::toLowerCase)
                .filter(d -> !d.isEmpty()).toList();
    }

    // Una lista vacía no restringe; se compara el dominio completo para que "sena.edu.co.atacante.com" no pase
    public static boolean correoPermitido(String correo, List<String> permitidos) {
        if (permitidos == null || permitidos.isEmpty()) {
            return true;
        }
        if (correo == null || !correo.contains("@")) {
            return false;
        }
        return permitidos.contains(correo.substring(correo.lastIndexOf('@') + 1).toLowerCase());
    }

    @Transactional
    public MensajeResponseDTO registrar(RegistroRequestDTO datos) {
        // Solo un admin con sesión elige cargo; el registro público siempre es de aprendiz
        String cargo = esAdminConSesion() && datos.cargo() != null && !datos.cargo().isBlank() ? datos.cargo().trim() : "Aprendiz";
        if (!Usuario.CARGOS_VALIDOS.contains(cargo)) {
            throw new DatoInvalidoException("El cargo seleccionado no es válido.");
        }
        // Ley 1581 de 2012: sin autorización expresa no se pueden tratar los datos personales
        if (!datos.aceptaDatos()) {
            throw new DatoInvalidoException("Debes autorizar el tratamiento de tus datos personales para poder registrarte.");
        }
        captchaService.validar(datos.captcha());

        String nombre = nombreLimpio(datos.nombre());
        String correo = datos.correo() == null ? "" : Texto.limpiar(datos.correo()).toLowerCase();
        if (nombre.isEmpty() || correo.isEmpty()) {
            throw new DatoInvalidoException("El nombre y el correo son obligatorios.");
        }
        authService.validarContrasena(datos.password(), datos.confirmacion() == null ? "" : datos.confirmacion());
        if (!correoPermitido(correo, dominios)) {
            throw new DatoInvalidoException("Solo se permiten correos de los dominios institucionales: " + String.join(", ", dominios));
        }
        String tipo = datos.tipoDocumento() == null || datos.tipoDocumento().isBlank()
                ? DocumentoService.TIPO_POR_DEFECTO : datos.tipoDocumento().trim().toUpperCase();
        String documento = datos.documento() == null || datos.documento().isBlank() ? null
                : documentoService.validar(tipo, datos.documento());
        if (usuarioRepository.existsByCorreoIgnoreCase(correo) || (documento != null && usuarioRepository.existsByDocumento(documento))) {
            throw new DatoInvalidoException(MENSAJE_NEUTRO_REGISTRO);
        }

        Usuario nuevo = new Usuario(nombre, correo, passwordEncoder.encode(datos.password()),
                rolRepository.findByNombre(Rol.USUARIO).orElseThrow(), cargo);
        nuevo.setTipoDocumento(tipo);
        nuevo.setDocumento(documento);
        if ("Aprendiz".equals(cargo)) {
            String ficha = Texto.opcional(datos.ficha());
            nuevo.setFicha(ficha);
            nuevo.setHorario(Texto.opcional(datos.horario()));
            nuevo.asignarFicha(ficha == null ? null : fichaRepository.findByNumero(ficha).orElse(null));
        }
        String codigo = codigo();
        nuevo.nuevoCodigoVerificacion(codigo, LocalDateTime.now(reloj).plusMinutes(MINUTOS_VIGENCIA));
        usuarioRepository.save(nuevo);
        if (!correoService.enviar(correo, "Código de verificación - Sistema de Acceso SENA",
                plantillas.verificacion(nombre, codigo, false))) {
            log.warn("No se pudo enviar el correo de verificación al usuario {}", nuevo.getId());
        }
        return new MensajeResponseDTO("¡Registro exitoso! Revisa tu correo.");
    }

    // Sin tope de intentos, un código de 6 dígitos se prueba hasta acertar
    @Transactional(noRollbackFor = DatoInvalidoException.class)
    public UsuarioSesionDTO verificarCorreo(String codigo) {
        Usuario yo = authService.usuarioActual();
        if (yo.isCorreoVerificado()) {
            throw new ConflictoException("Tu correo ya está verificado.");
        }
        if (yo.getIntentosCodigo() >= MAX_INTENTOS_CODIGO) {
            yo.anularCodigoVerificacion();
            throw new DatoInvalidoException("Demasiados intentos fallidos. Solicita un código nuevo.");
        }
        if (!iguales(yo.getCodigoVerificacion(), codigo)) {
            int restantes = Math.max(MAX_INTENTOS_CODIGO - yo.sumarIntentoCodigo(), 0);
            throw new DatoInvalidoException("Código incorrecto. Te quedan " + restantes + " intentos.");
        }
        if (yo.getCodigoExpiracion() != null && LocalDateTime.now(reloj).isAfter(yo.getCodigoExpiracion())) {
            throw new DatoInvalidoException("El código ha expirado. Por favor, solicita uno nuevo.");
        }
        yo.verificarCorreo();
        return UsuarioSesionDTO.fromEntity(yo);
    }

    @Transactional
    public MensajeResponseDTO reenviarCodigo() {
        Usuario yo = authService.usuarioActual();
        if (yo.isCorreoVerificado()) {
            throw new ConflictoException("Tu correo ya está verificado.");
        }
        String codigo = codigo();
        yo.nuevoCodigoVerificacion(codigo, LocalDateTime.now(reloj).plusMinutes(MINUTOS_VIGENCIA));
        correoService.enviar(yo.getCorreo(), "Nuevo código de verificación - Sistema de Acceso SENA",
                plantillas.verificacion(yo.getNombre(), codigo, true));
        return new MensajeResponseDTO("Se ha reenviado un nuevo código a tu correo.");
    }

    // La respuesta es la misma exista o no la cuenta; los contadores del login no se tocan
    @Transactional
    public MensajeResponseDTO solicitarRecuperacion(String correo, String captcha) {
        captchaService.validar(captcha);
        String limpio = correo.trim().toLowerCase();
        usuarioRepository.buscarPorCorreoODocumento(limpio).filter(u -> u.getCorreo().equalsIgnoreCase(limpio))
                .filter(u -> admiteOtroCodigo(limpio)).ifPresent(u -> {
                    String codigo = codigo();
                    u.nuevoCodigoRecuperacion(codigo, LocalDateTime.now(reloj).plusMinutes(MINUTOS_VIGENCIA));
                    correoService.enviar(u.getCorreo(), "Recuperación de contraseña - SENA",
                            plantillas.recuperacion(u.getNombre(), codigo));
                    log.info("Código de recuperación emitido para el usuario {}", u.getId());
                });
        return new MensajeResponseDTO(MENSAJE_NEUTRO_RECUPERACION);
    }

    // Fallar códigos no bloquea el login: si no, cualquiera dejaría al celador de turno fuera del sistema
    @Transactional(noRollbackFor = DatoInvalidoException.class)
    public PermisoRecuperacionResponseDTO verificarRecuperacion(String correo, String codigo) {
        Usuario usuario = porCorreo(correo);
        if (usuario == null || usuario.getCodigoRecuperacion() == null) {
            throw new DatoInvalidoException("El código ingresado es incorrecto o ya fue usado.");
        }
        if (usuario.getRecuperacionExpiracion() != null && LocalDateTime.now(reloj).isAfter(usuario.getRecuperacionExpiracion())) {
            usuario.anularRecuperacion();
            throw new DatoInvalidoException("El código expiró. Solicita uno nuevo.");
        }
        if (!iguales(usuario.getCodigoRecuperacion(), codigo)) {
            int intentos = usuario.sumarIntentoCodigo();
            if (intentos >= MAX_INTENTOS_CODIGO) {
                usuario.anularRecuperacion();
                log.warn("Código de recuperación anulado por exceso de intentos (usuario {})", usuario.getId());
                throw new DatoInvalidoException("Demasiados intentos fallidos. El código fue anulado, solicita uno nuevo.");
            }
            throw new DatoInvalidoException("El código ingresado es incorrecto. Te quedan "
                    + (MAX_INTENTOS_CODIGO - intentos) + " intentos.");
        }
        byte[] bytes = new byte[32];
        azar.nextBytes(bytes);
        String permiso = HexFormat.of().formatHex(bytes);
        usuario.concederPermisoRecuperacion(permiso);
        return new PermisoRecuperacionResponseDTO("Código verificado. Ahora puedes cambiar tu contraseña.", permiso);
    }

    @Transactional
    public MensajeResponseDTO cambiarPorRecuperacion(RecuperacionCambioRequestDTO datos) {
        Usuario usuario = porCorreo(datos.correo());
        if (usuario == null || usuario.getRecuperacionPermiso() == null || !iguales(usuario.getRecuperacionPermiso(), datos.permiso())
                || (usuario.getRecuperacionExpiracion() != null && LocalDateTime.now(reloj).isAfter(usuario.getRecuperacionExpiracion()))) {
            throw new DatoInvalidoException("Debes verificar tu código antes de cambiar la contraseña.");
        }
        authService.validarContrasena(datos.password(), datos.confirmacion() == null ? "" : datos.confirmacion());
        usuario.restablecerContrasena(passwordEncoder.encode(datos.password()));
        log.info("Contraseña restablecida para el usuario {}", usuario.getId());
        return new MensajeResponseDTO("Tu contraseña fue actualizada. Ya puedes iniciar sesión.");
    }

    private Usuario porCorreo(String correo) {
        String limpio = correo == null ? "" : correo.trim().toLowerCase();
        return usuarioRepository.buscarPorCorreoODocumento(limpio).filter(u -> u.getCorreo().equalsIgnoreCase(limpio)).orElse(null);
    }

    // Además del límite por IP, cada buzón recibe pocos códigos por hora: evita inundar el correo de otro
    private boolean admiteOtroCodigo(String correo) {
        long ahora = reloj.millis();
        Deque<Long> envios = codigosPorCorreo.computeIfAbsent(correo, c -> new ArrayDeque<>());
        synchronized (envios) {
            envios.removeIf(t -> ahora - t > 3_600_000);
            if (envios.size() >= CODIGOS_POR_HORA_POR_CORREO) {
                return false;
            }
            envios.add(ahora);
            return true;
        }
    }

    private boolean esAdminConSesion() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ADMIN".equals(a.getAuthority()));
    }

    private String codigo() {
        return String.format("%06d", azar.nextInt(1_000_000));
    }

    private static String nombreLimpio(String nombre) {
        if (nombre == null) {
            return "";
        }
        String limpio = nombre.replaceAll("[^\\p{L}\\s]", "").trim().replaceAll("\\s+", " ");
        StringBuilder titulo = new StringBuilder();
        for (String palabra : limpio.split(" ")) {
            if (!palabra.isEmpty()) {
                titulo.append(titulo.isEmpty() ? "" : " ").append(Character.toUpperCase(palabra.charAt(0)))
                        .append(palabra.substring(1).toLowerCase());
            }
        }
        return titulo.toString();
    }

    private static boolean iguales(String guardado, String recibido) {
        if (guardado == null || recibido == null) {
            return false;
        }
        return MessageDigest.isEqual(guardado.getBytes(StandardCharsets.UTF_8), recibido.trim().getBytes(StandardCharsets.UTF_8));
    }
}
