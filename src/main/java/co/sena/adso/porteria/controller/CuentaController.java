package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.CodigoRequestDTO;
import co.sena.adso.porteria.dto.MensajeResponseDTO;
import co.sena.adso.porteria.dto.PermisoRecuperacionResponseDTO;
import co.sena.adso.porteria.dto.RecuperacionCambioRequestDTO;
import co.sena.adso.porteria.dto.RecuperacionCodigoRequestDTO;
import co.sena.adso.porteria.dto.RecuperacionRequestDTO;
import co.sena.adso.porteria.dto.RegistroRequestDTO;
import co.sena.adso.porteria.dto.UsuarioSesionDTO;
import co.sena.adso.porteria.service.CaptchaService;
import co.sena.adso.porteria.service.CuentaService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class CuentaController {

    private final CuentaService cuentaService;
    private final CaptchaService captchaService;

    public CuentaController(CuentaService cuentaService, CaptchaService captchaService) {
        this.cuentaService = cuentaService;
        this.captchaService = captchaService;
    }

    @GetMapping("/captcha")
    public Map<String, Object> captcha() {
        return captchaService.crearDesafio();
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public MensajeResponseDTO registrar(@Valid @RequestBody RegistroRequestDTO datos) {
        return cuentaService.registrar(datos);
    }

    @PostMapping("/verificacion")
    public UsuarioSesionDTO verificar(@Valid @RequestBody CodigoRequestDTO datos) {
        return cuentaService.verificarCorreo(datos.codigo());
    }

    @PostMapping("/verificacion/reenviar")
    public MensajeResponseDTO reenviar() {
        return cuentaService.reenviarCodigo();
    }

    @PostMapping("/recuperacion")
    public MensajeResponseDTO recuperar(@Valid @RequestBody RecuperacionRequestDTO datos) {
        return cuentaService.solicitarRecuperacion(datos.correo(), datos.captcha());
    }

    @PostMapping("/recuperacion/verificar")
    public PermisoRecuperacionResponseDTO verificarRecuperacion(@Valid @RequestBody RecuperacionCodigoRequestDTO datos) {
        return cuentaService.verificarRecuperacion(datos.correo(), datos.codigo());
    }

    @PostMapping("/recuperacion/cambiar")
    public MensajeResponseDTO cambiarContrasena(@Valid @RequestBody RecuperacionCambioRequestDTO datos) {
        return cuentaService.cambiarPorRecuperacion(datos);
    }
}
