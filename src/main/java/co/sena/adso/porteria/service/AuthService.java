package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.CambioContrasenaRequestDTO;
import co.sena.adso.porteria.dto.LoginRequestDTO;
import co.sena.adso.porteria.dto.SesionResponseDTO;
import co.sena.adso.porteria.dto.UsuarioSesionDTO;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.exception.BusinessException;
import co.sena.adso.porteria.exception.CredencialesInvalidasException;
import co.sena.adso.porteria.exception.CuentaBloqueadaException;
import co.sena.adso.porteria.exception.DatoInvalidoException;
import co.sena.adso.porteria.repository.UsuarioRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public static final int MAX_INTENTOS = 5;
    public static final int MINUTOS_BLOQUEO = 10;
    public static final int LONGITUD_MINIMA = 8;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock reloj;
    private final SecureRandom aleatorio = new SecureRandom();

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.reloj = reloj;
    }

    // noRollbackFor: el intento fallido debe quedar guardado aunque se responda con error
    @Transactional(noRollbackFor = {CredencialesInvalidasException.class, CuentaBloqueadaException.class})
    public SesionResponseDTO login(LoginRequestDTO datos) {
        String identificador = normalizarIdentificador(datos.identificador());
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Usuario usuario = usuarioRepository.buscarPorCorreoODocumento(identificador).orElse(null);

        if (usuario != null && usuario.estaBloqueado(ahora)) {
            long segundos = Duration.between(ahora, usuario.getBloqueadoHasta()).toSeconds();
            throw new CuentaBloqueadaException("Cuenta bloqueada temporalmente por demasiados intentos", segundos);
        }
        if (usuario == null || !passwordEncoder.matches(datos.password(), usuario.getContrasena())) {
            if (usuario != null) {
                usuario.registrarIntentoFallido(MAX_INTENTOS, ahora.plusMinutes(MINUTOS_BLOQUEO));
                if (usuario.estaBloqueado(ahora)) {
                    log.warn("Cuenta {} bloqueada por intentos fallidos", usuario.getId());
                    throw new CuentaBloqueadaException("Bloqueada por " + MINUTOS_BLOQUEO
                            + " minutos. Demasiados intentos", MINUTOS_BLOQUEO * 60L);
                }
            }
            throw new CredencialesInvalidasException();
        }

        usuario.limpiarBloqueo();
        // Un token de sesión nuevo invalida cualquier otra sesión abierta de la misma cuenta
        usuario.setSessionToken(nuevoTokenSesion());
        log.info("Inicio de sesión del usuario {}", usuario.getId());
        return sesion(usuario);
    }

    @Transactional
    public void logout() {
        usuarioActual().setSessionToken(null);
    }

    @Transactional(readOnly = true)
    public SesionResponseDTO renovar() {
        return sesion(usuarioActual());
    }

    @Transactional(readOnly = true)
    public UsuarioSesionDTO yo() {
        return UsuarioSesionDTO.fromEntity(usuarioActual());
    }

    @Transactional
    public SesionResponseDTO cambiarContrasena(CambioContrasenaRequestDTO datos) {
        Usuario usuario = usuarioActual();
        if (!usuario.isDebeCambiarContrasena()) {
            throw new AccessDeniedException("Solo se cambia aquí una contraseña temporal");
        }
        // Se pide la actual: quien conociera la temporal no puede apropiarse de la cuenta
        if (!passwordEncoder.matches(datos.actual(), usuario.getContrasena())) {
            throw new DatoInvalidoException("La contraseña actual no es correcta");
        }
        validarContrasena(datos.nueva(), datos.confirmacion());
        if (passwordEncoder.matches(datos.nueva(), usuario.getContrasena())) {
            throw new BusinessException("La nueva contraseña debe ser distinta de la actual");
        }
        usuario.setContrasena(passwordEncoder.encode(datos.nueva()));
        usuario.setDebeCambiarContrasena(false);
        usuario.setSessionToken(nuevoTokenSesion());
        return sesion(usuario);
    }

    public void validarContrasena(String contrasena, String confirmacion) {
        if (contrasena == null || contrasena.isBlank()) {
            throw new DatoInvalidoException("Debes escribir una contraseña");
        }
        if (confirmacion != null && !contrasena.equals(confirmacion)) {
            throw new DatoInvalidoException("Las contraseñas no coinciden");
        }
        if (contrasena.length() < LONGITUD_MINIMA) {
            throw new DatoInvalidoException("La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres");
        }
        boolean letras = contrasena.chars().anyMatch(Character::isLetter);
        boolean numeros = contrasena.chars().anyMatch(Character::isDigit);
        if (!letras || !numeros) {
            throw new DatoInvalidoException("La contraseña debe combinar letras y números");
        }
    }

    /** Usuario de la petición en curso; el filtro JWT ya validó token y sesión. */
    public Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long id)) {
            throw new AuthenticationCredentialsNotFoundException("Sin sesión");
        }
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Sin sesión"));
    }

    private SesionResponseDTO sesion(Usuario usuario) {
        return new SesionResponseDTO(jwtService.generar(usuario), jwtService.duracionPara(usuario),
                UsuarioSesionDTO.fromEntity(usuario));
    }

    private String nuevoTokenSesion() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String normalizarIdentificador(String valor) {
        String limpio = valor.trim();
        return limpio.contains("@") ? limpio.toLowerCase() : limpio;
    }
}
