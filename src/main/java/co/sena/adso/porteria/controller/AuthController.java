package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.CambioContrasenaRequestDTO;
import co.sena.adso.porteria.dto.LoginRequestDTO;
import co.sena.adso.porteria.dto.SesionResponseDTO;
import co.sena.adso.porteria.dto.UsuarioSesionDTO;
import co.sena.adso.porteria.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public SesionResponseDTO login(@Valid @RequestBody LoginRequestDTO datos) {
        return authService.login(datos);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        authService.logout();
    }

    @PostMapping("/renovar")
    public SesionResponseDTO renovar() {
        return authService.renovar();
    }

    @GetMapping("/yo")
    public UsuarioSesionDTO yo() {
        return authService.yo();
    }

    @PostMapping("/cambiar-contrasena")
    public SesionResponseDTO cambiarContrasena(@Valid @RequestBody CambioContrasenaRequestDTO datos) {
        return authService.cambiarContrasena(datos);
    }
}
