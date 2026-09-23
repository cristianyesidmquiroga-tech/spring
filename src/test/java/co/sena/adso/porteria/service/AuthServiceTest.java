package co.sena.adso.porteria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import co.sena.adso.porteria.dto.LoginRequestDTO;
import co.sena.adso.porteria.dto.SesionResponseDTO;
import co.sena.adso.porteria.entity.Rol;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.exception.CredencialesInvalidasException;
import co.sena.adso.porteria.exception.CuentaBloqueadaException;
import co.sena.adso.porteria.exception.DatoInvalidoException;
import co.sena.adso.porteria.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-09-23T13:00:00Z"), ZoneId.of("America/Bogota"));
        authService = new AuthService(usuarioRepository, passwordEncoder, jwtService, reloj);
        Rol rol = BeanUtils.instantiateClass(Rol.class);
        ReflectionTestUtils.setField(rol, "nombre", Rol.USUARIO);
        usuario = new Usuario("Laura", "laura@porteria.local", "hash", rol, "Aprendiz");
        ReflectionTestUtils.setField(usuario, "id", 7L);
    }

    @Test
    void elQuintoIntentoFallidoBloqueaLaCuenta() {
        when(usuarioRepository.buscarPorCorreoODocumento("laura@porteria.local")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        LoginRequestDTO malo = new LoginRequestDTO("Laura@Porteria.local ", "mala1234");

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authService.login(malo)).isInstanceOf(CredencialesInvalidasException.class);
        }
        assertThatThrownBy(() -> authService.login(malo)).isInstanceOf(CuentaBloqueadaException.class);
        assertThat(usuario.getBloqueadoHasta()).isNotNull();
    }

    @Test
    void cuentaBloqueadaNoEntraNiConLaClaveCorrecta() {
        when(usuarioRepository.buscarPorCorreoODocumento("laura@porteria.local")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        LoginRequestDTO malo = new LoginRequestDTO("laura@porteria.local", "mala1234");
        for (int i = 0; i < 5; i++) {
            try {
                authService.login(malo);
            } catch (RuntimeException ignorada) {
                // se esperan los fallos
            }
        }
        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("laura@porteria.local", "buena123")))
                .isInstanceOf(CuentaBloqueadaException.class);
    }

    @Test
    void usuarioInexistenteRespondeIgualQueClaveIncorrecta() {
        when(usuarioRepository.buscarPorCorreoODocumento("nadie@porteria.local")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("nadie@porteria.local", "x1234567")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo/documento o contraseña incorrectos");
    }

    @Test
    void loginCorrectoGeneraSesionNuevaYLimpiaIntentos() {
        ReflectionTestUtils.setField(usuario, "intentosFallidos", 3);
        when(usuarioRepository.buscarPorCorreoODocumento("1098000001")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("buena123", "hash")).thenReturn(true);
        when(jwtService.generar(usuario)).thenReturn("token");

        SesionResponseDTO sesion = authService.login(new LoginRequestDTO("1098000001", "buena123"));

        assertThat(sesion.token()).isEqualTo("token");
        assertThat(usuario.getSessionToken()).hasSize(64);
        assertThat(usuario.getIntentosFallidos()).isZero();
    }

    @Test
    void laContrasenaDebeCombinarLetrasYNumeros() {
        assertThatThrownBy(() -> authService.validarContrasena("solamenteletras", null))
                .isInstanceOf(DatoInvalidoException.class);
        assertThatThrownBy(() -> authService.validarContrasena("12345678", null))
                .isInstanceOf(DatoInvalidoException.class);
        assertThatThrownBy(() -> authService.validarContrasena("Corta1", null))
                .isInstanceOf(DatoInvalidoException.class);
        authService.validarContrasena("Segura2026", "Segura2026");
    }
}
