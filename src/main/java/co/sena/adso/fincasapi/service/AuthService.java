package co.sena.adso.fincasapi.service;

import co.sena.adso.fincasapi.dto.LoginRequestDTO;
import co.sena.adso.fincasapi.dto.TokenResponseDTO;
import co.sena.adso.fincasapi.entity.Usuario;
import co.sena.adso.fincasapi.exception.CredencialesInvalidasException;
import co.sena.adso.fincasapi.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public TokenResponseDTO login(LoginRequestDTO datos) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(datos.email().trim())
                .filter(u -> passwordEncoder.matches(datos.password(), u.getPasswordHash()))
                .orElseThrow(CredencialesInvalidasException::new);
        String token = jwtService.generarToken(usuario.getEmail(), usuario.getRol().name());
        return new TokenResponseDTO(token, "Bearer", jwtService.getExpiracionMs());
    }
}
