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
            new Regla("POST", "/api/perfil/foto", List.of(new Ventana(6, MINUTO), new Ventana(40, HORA))),
            new Regla("GET", "/api/porteria/panel/exportar", List.of(new Ventana(10, HORA))),
            new Regla("GET", "/api/historial", List.of(new Ventana(60, MINUTO))),
            new Regla("POST", "/api/equipos", List.of(new Ventana(20, HORA))),
            // Una auditoría envió 50 mensajes seguidos por el centro de ayuda sin ninguna traba
            new Regla("POST", "/api/mensajes", List.of(new Ventana(5, MINUTO), new Ventana(40, HORA))),
            new Regla("POST", "/api/ayuda/contacto", List.of(new Ventana(5, HORA), new Ventana(15, 24 * HORA))));

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
        long esperaMs = 0;
        synchronized (lista) {
            while (!lista.isEmpty() && ahora - lista.peekFirst() > ventanaMayor) {
                lista.pollFirst();
            }
            for (Ventana v : regla.ventanas()) {
                List<Long> dentro = lista.stream().filter(t -> ahora - t <= v.duracionMs()).toList();
                if (dentro.size() >= v.maximo()) {
                    esperaMs = Math.max(esperaMs, v.duracionMs() - (ahora - dentro.get(0)));
                }
            }
            if (esperaMs == 0) {
                lista.addLast(ahora);
            }
        }
        limpiarSiCrece(ahora);

        if (esperaMs > 0) {
            long segundos = Math.max(1, (esperaMs + 999) / 1000);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(segundos));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"status\":429,\"mensaje\":\"Hiciste demasiadas peticiones seguidas. Espera "
                    + cuanto(segundos) + " y vuelve a intentarlo.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static String cuanto(long segundos) {
        if (segundos <= 90) {
            return "un minuto";
        }
        if (segundos < 3600) {
            return Math.round(segundos / 60.0) + " minutos";
        }
        return segundos < 5400 ? "una hora" : Math.round(segundos / 3600.0) + " horas";
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
