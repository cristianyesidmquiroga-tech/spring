package co.sena.adso.porteria.service;

import co.sena.adso.porteria.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    // Quien opera portería trabaja un turno completo; el resto se cierra tras 10 minutos sin renovar
    public static final long DURACION_PORTERIA_MS = 12L * 60 * 60 * 1000;
    public static final long DURACION_NORMAL_MS = 10L * 60 * 1000;

    private final SecretKey clave;

    public JwtService(@Value("${jwt.secret}") String secreto) {
        // hmacShaKeyFor rechaza claves de menos de 256 bits: un secreto débil falla al arrancar
        this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secreto));
    }

    public record DatosToken(Long usuarioId, String sesion) {
    }

    public long duracionPara(Usuario usuario) {
        return usuario.puedeOperarPorteria() ? DURACION_PORTERIA_MS : DURACION_NORMAL_MS;
    }

    public String generar(Usuario usuario) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("sid", usuario.getSessionToken())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + duracionPara(usuario)))
                .signWith(clave)
                .compact();
    }

    public Optional<DatosToken> validar(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
            return Optional.of(new DatosToken(Long.valueOf(claims.getSubject()), claims.get("sid", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
