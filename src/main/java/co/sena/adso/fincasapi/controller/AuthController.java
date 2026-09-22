package co.sena.adso.fincasapi.controller;

import co.sena.adso.fincasapi.dto.LoginRequestDTO;
import co.sena.adso.fincasapi.dto.RegistroRequestDTO;
import co.sena.adso.fincasapi.dto.TokenResponseDTO;
import co.sena.adso.fincasapi.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponseDTO registrar(@Valid @RequestBody RegistroRequestDTO datos) {
        return authService.registrar(datos);
    }

    @PostMapping("/login")
    public TokenResponseDTO login(@Valid @RequestBody LoginRequestDTO datos) {
        return authService.login(datos);
    }
}
