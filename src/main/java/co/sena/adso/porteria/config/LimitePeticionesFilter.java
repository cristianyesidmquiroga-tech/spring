package co.sena.adso.porteria.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Catálogo único de límites de peticiones. Sin sesión se cuenta por IP; con sesión, por usuario.
 * El login además tiene bloqueo por cuenta; este límite frena probar muchas cuentas desde un mismo equipo.
 */
@Component
public class LimitePeticionesFilter extends OncePerRequestFilter {

    private static final long MINUTO = 60_000;
    private static final long HORA = 60 * MINUTO;

    private record Ventana(int maximo, long duracionMs) {
    }

    private record Regla(String metodo, String ruta, List<Ventana> ventanas) {
        boolean aplica(HttpServletRequest req) {
            return metodo.equals(req.getMethod()) && ruta.equals(req.getRequestURI());
        }
    }

    private static final List<Regla> LIMITES = List.of(
            new Regla("POST", "/api/auth/login", List.of(new Ventana(10, MINUTO), new Ventana(80, HORA))),
            new Regla("POST", "/api/auth/cambiar-contrasena", List.of(new Ventana(5, MINUTO), new Ventana(20, HORA))),
            new Regla("PUT", "/api/perfil", List.of(new Ventana(6, MINUTO), new Ventana(40, HORA))),
            new Regla("POST", "/api/perfil/foto", List.of(new Ventana(6, MINUTO), new Ventana(40, HORA))));

    private final Map<String, Deque<Long>> registros = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return LIMITES.stream().noneMatch(r -> r.aplica(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Regla regla = LIMITES.stream().filter(r -> r.aplica(request)).findFirst().orElseThrow();
        String clave = regla.metodo() + " " + regla.ruta() + " " + quien(request);
        long ahora = System.currentTimeMillis();
        long ventanaMayor = regla.ventanas().stream().mapToLong(Ventana::duracionMs).max().orElse(MINUTO);

        Deque<Long> lista = registros.computeIfAbsent(clave, k -> new ArrayDeque<>());
        boolean excedido;
        synchronized (lista) {
            while (!lista.isEmpty() && ahora - lista.peekFirst() > ventanaMayor) {
                lista.pollFirst();
            }
            excedido = regla.ventanas().stream().anyMatch(v ->
                    lista.stream().filter(t -> ahora - t <= v.duracionMs()).count() >= v.maximo());
            if (!excedido) {
                lista.addLast(ahora);
            }
        }
        limpiarSiCrece(ahora);

        if (excedido) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"status\":429,\"mensaje\":\"Demasiadas solicitudes, espera un momento\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static String quien(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long id) {
            return "u" + id;
        }
        return "ip" + request.getRemoteAddr();
    }

    private void limpiarSiCrece(long ahora) {
        if (registros.size() > 10_000) {
            registros.entrySet().removeIf(e -> e.getValue().isEmpty() || ahora - e.getValue().peekLast() > HORA);
        }
    }
}
