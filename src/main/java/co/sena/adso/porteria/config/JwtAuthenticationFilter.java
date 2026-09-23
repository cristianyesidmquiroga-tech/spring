package co.sena.adso.porteria.config;

import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.repository.UsuarioRepository;
import co.sena.adso.porteria.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera == null || !cabecera.startsWith(PREFIJO)) {
            chain.doFilter(request, response);
            return;
        }
        Usuario usuario = jwtService.validar(cabecera.substring(PREFIJO.length()))
                .flatMap(datos -> usuarioRepository.findById(datos.usuarioId())
                        .filter(u -> mismaSesion(u.getSessionToken(), datos.sesion())))
                .orElse(null);

        if (usuario != null) {
            // Con contraseña temporal solo se permite cambiarla o cerrar sesión
            if (usuario.isDebeCambiarContrasena() && !request.getRequestURI().startsWith("/api/auth/")) {
                responderCambioPendiente(response);
                return;
            }
            var auth = new UsernamePasswordAuthenticationToken(usuario.getId(), null, permisos(usuario));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }

    // Si el token de sesión guardado cambió (login en otro equipo, logout, cambio de clave) el JWT deja de servir
    private static boolean mismaSesion(String guardada, String recibida) {
        if (guardada == null || recibida == null) {
            return false;
        }
        return MessageDigest.isEqual(guardada.getBytes(StandardCharsets.UTF_8), recibida.getBytes(StandardCharsets.UTF_8));
    }

    private static List<SimpleGrantedAuthority> permisos(Usuario u) {
        List<SimpleGrantedAuthority> lista = new ArrayList<>();
        lista.add(new SimpleGrantedAuthority("USUARIO"));
        if (u.esAdmin()) lista.add(new SimpleGrantedAuthority("ADMIN"));
        if (u.puedeOperarPorteria()) lista.add(new SimpleGrantedAuthority("OPERAR_PORTERIA"));
        if (u.puedeAsesorar()) lista.add(new SimpleGrantedAuthority("ASESORAR"));
        if (u.puedeGestionarAsistencia()) lista.add(new SimpleGrantedAuthority("GESTIONAR_ASISTENCIA"));
        if (u.puedeRegistrarEquipos()) lista.add(new SimpleGrantedAuthority("REGISTRAR_EQUIPOS"));
        if (u.puedeVerAmbientes()) lista.add(new SimpleGrantedAuthority("VER_AMBIENTES"));
        return lista;
    }

    private static void responderCambioPendiente(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":403,\"codigo\":\"CAMBIO_CONTRASENA\","
                + "\"mensaje\":\"Debes cambiar tu contraseña temporal antes de continuar\"}");
    }
}
