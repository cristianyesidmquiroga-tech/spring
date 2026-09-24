package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.HistorialResponseDTO;
import co.sena.adso.porteria.dto.HistorialResponseDTO.Bloque;
import co.sena.adso.porteria.dto.HistorialResponseDTO.DetalleDia;
import co.sena.adso.porteria.dto.HistorialResponseDTO.Movimiento;
import co.sena.adso.porteria.dto.HistorialResponseDTO.Resumen;
import co.sena.adso.porteria.entity.Acceso;
import co.sena.adso.porteria.entity.Equipo;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.repository.AccesoRepository;
import co.sena.adso.porteria.repository.EquipoRepository;
import co.sena.adso.porteria.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Historial de ingresos por persona o por grupo, con el resumen de asistencia.
 * Reglas que vienen del sistema original:
 * - El periodo exigible es por persona: nadie falta antes de su primer acceso ni después de terminar su ficha.
 * - Un día con marca de entrada o de salida cuenta como asistido (el turno nocturno sale de madrugada).
 * - El porcentaje solo cuenta días dentro del calendario evaluado; si no, asistir un sábado daría más de 100%.
 */
@Service
public class HistorialService {

    private static final int DIAS_POR_DEFECTO = 30;
    // Topes para que una consulta grande no tumbe el servidor
    private static final int MAXIMO_DIAS_RANGO = 120;
    private static final int MAXIMO_PERSONAS = 300;
    private static final long MAXIMO_ACCESOS = 20_000;
    private static final int MAXIMO_MOVIMIENTOS_LISTADOS = 500;
    // Se lee un día más por cada lado para no partir parejas entrada-salida en el borde del rango
    private static final int MARGEN_DIAS = 1;
    private static final LocalTime HORA_CIERRE_AUTOMATICO = LocalTime.of(23, 59, 59);
    // Con menos de 3 lunes en el periodo, faltar a uno no es un patrón
    private static final int MINIMO_OPORTUNIDADES_DIA = 3;
    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    private final AccesoRepository accesoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EquipoRepository equipoRepository;
    private final AuthService authService;
    private final Clock reloj;

    public HistorialService(AccesoRepository accesoRepository, UsuarioRepository usuarioRepository,
                            EquipoRepository equipoRepository, AuthService authService, Clock reloj) {
        this.accesoRepository = accesoRepository;
        this.usuarioRepository = usuarioRepository;
        this.equipoRepository = equipoRepository;
        this.authService = authService;
        this.reloj = reloj;
    }

    public record Filtros(List<Long> usuarioIds, String ficha, String cargo, String busqueda,
                          LocalDate fechaInicio, LocalDate fechaFin, boolean soloHabiles) {
    }

    @Transactional(readOnly = true)
    public HistorialResponseDTO consultar(Filtros pedido) {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate fin = pedido.fechaFin() == null ? hoy : pedido.fechaFin();
        LocalDate inicio = pedido.fechaInicio() == null ? fin.minusDays(DIAS_POR_DEFECTO - 1L) : pedido.fechaInicio();
        if (inicio.isAfter(fin)) {
            LocalDate cambio = inicio;
            inicio = fin;
            fin = cambio;
        }
        boolean recortado = false;
        if (ChronoUnit.DAYS.between(inicio, fin) + 1 > MAXIMO_DIAS_RANGO) {
            inicio = fin.minusDays(MAXIMO_DIAS_RANGO - 1L);
            recortado = true;
        }

        List<Long> ids = pedido.usuarioIds() == null ? List.of() : pedido.usuarioIds();
        String ficha = texto(pedido.ficha());
        String cargo = texto(pedido.cargo());
        String busqueda = texto(pedido.busqueda());

        Usuario solicitante = authService.usuarioActual();
        if (!(solicitante.puedeOperarPorteria() || solicitante.puedeGestionarAsistencia())) {
            // Cualquiera ve su propio historial; pedir el de otra persona es un 403, no un filtro silencioso
            if (ids.stream().anyMatch(id -> !id.equals(solicitante.getId()))) {
                throw new AccessDeniedException("Solo puedes consultar tu propio historial");
            }
            ids = List.of(solicitante.getId());
            ficha = "";
            cargo = "";
            busqueda = "";
        }

        if (ids.isEmpty() && ficha.isEmpty() && cargo.isEmpty() && busqueda.isEmpty()) {
            return new HistorialResponseDTO(inicio, fin, recortado, pedido.soloHabiles(), List.of(), List.of());
        }

        List<Usuario> personas = usuarioRepository.buscarParaHistorial(!ids.isEmpty(), ids.isEmpty() ? List.of(-1L) : ids,
                ficha, cargo, patron(busqueda), PageRequest.of(0, MAXIMO_PERSONAS));
        return construir(personas, inicio, fin, pedido.soloHabiles(), recortado, hoy);
    }

    private HistorialResponseDTO construir(List<Usuario> personas, LocalDate inicio, LocalDate fin, boolean soloHabiles,
                                           boolean recortado, LocalDate hoy) {
        if (personas.isEmpty()) {
            return new HistorialResponseDTO(inicio, fin, recortado, soloHabiles, List.of(), List.of());
        }
        LocalDateTime desde = inicio.minusDays(MARGEN_DIAS).atStartOfDay();
        LocalDateTime hasta = fin.plusDays(MARGEN_DIAS).atTime(LocalTime.MAX);

        // Se cuentan los accesos antes de traerlos: mejor dejar fuera a alguien y decirlo que traerlo a medias
        Map<Long, Long> conteos = new HashMap<>();
        for (Object[] fila : accesoRepository.contarPorUsuario(ids(personas), desde, hasta)) {
            conteos.put((Long) fila[0], (Long) fila[1]);
        }
        List<Usuario> analizadas = new ArrayList<>();
        List<String> omitidas = new ArrayList<>();
        long acumulado = 0;
        for (Usuario p : personas) {
            long cuantos = conteos.getOrDefault(p.getId(), 0L);
            if (!analizadas.isEmpty() && acumulado + cuantos > MAXIMO_ACCESOS) {
                omitidas.add(p.getNombre());
                continue;
            }
            acumulado += cuantos;
            analizadas.add(p);
        }

        Map<Long, LocalDate> primerAcceso = new HashMap<>();
        for (Object[] fila : accesoRepository.primerAccesoPorUsuario(ids(analizadas))) {
            primerAcceso.put((Long) fila[0], ((LocalDateTime) fila[1]).toLocalDate());
        }
        List<Acceso> accesos = accesoRepository.accesosDeUsuarios(ids(analizadas), desde, hasta);
        Map<Long, String> equipos = equipoRepository.findAllById(accesos.stream()
                        .flatMap(a -> a.listaEquipos().stream()).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Equipo::getId, Equipo::getNombre));
        Map<Long, List<Acceso>> porPersona = accesos.stream()
                .collect(Collectors.groupingBy(Acceso::getReferenciaId, LinkedHashMap::new, Collectors.toList()));

        List<Bloque> bloques = new ArrayList<>();
        for (Usuario p : analizadas) {
            List<Mov> movimientos = recortar(emparejar(porPersona.getOrDefault(p.getId(), List.of()), equipos, hoy), inicio, fin);

            LocalDate exigibleInicio = inicio;
            LocalDate exigibleFin = fin;
            List<String> motivos = new ArrayList<>();
            LocalDate primero = primerAcceso.get(p.getId());
            if (primero == null) {
                motivos.add("sin_referencia_de_vinculacion");
            } else if (primero.isAfter(exigibleInicio)) {
                exigibleInicio = primero;
                motivos.add("vinculacion");
            }
            LocalDate finFicha = p.getFichaRef() == null ? null : p.getFichaRef().getFechaFinalizacion();
            if (finFicha != null && finFicha.isBefore(exigibleFin)) {
                exigibleFin = finFicha;
                motivos.add("fin_de_ficha");
            }
            if (hoy.isBefore(exigibleFin)) {
                exigibleFin = hoy;
            }
            List<LocalDate> esperados = exigibleInicio.isAfter(exigibleFin) ? List.of()
                    : diasEsperados(exigibleInicio, exigibleFin, soloHabiles);

            List<Movimiento> listados = movimientos.subList(Math.max(0, movimientos.size() - MAXIMO_MOVIMIENTOS_LISTADOS),
                    movimientos.size()).stream().map(m -> m.aDto(inicio, fin)).toList();
            bloques.add(new Bloque(p.getId(), p.getNombre(), p.getDocumento(), p.getCargo(), p.numeroFicha(),
                    listados, resumir(movimientos, esperados, motivos)));
        }
        return new HistorialResponseDTO(inicio, fin, recortado, soloHabiles, bloques, omitidas);
    }

    /** Empareja entrada y salida en orden. Soporta entrada sin salida, dos entradas seguidas y salida sin entrada. */
    private List<Mov> emparejar(List<Acceso> accesos, Map<Long, String> equipos, LocalDate hoy) {
        List<Mov> movimientos = new ArrayList<>();
        Mov pendiente = null;
        for (Acceso a : accesos) {
            List<String> nombres = a.listaEquipos().stream()
                    .map(id -> equipos.getOrDefault(id, "Equipo eliminado (#" + id + ")")).toList();
            if (Acceso.ENTRADA.equals(a.getTipo())) {
                if (pendiente != null) {
                    movimientos.add(pendiente);
                }
                pendiente = new Mov(a.getFecha(), null, new ArrayList<>(nombres), false, false);
            } else if (pendiente == null) {
                movimientos.add(new Mov(null, a.getFecha(), new ArrayList<>(nombres), esCierreAutomatico(a), true));
            } else {
                pendiente.salida = a.getFecha();
                pendiente.cierreAutomatico = esCierreAutomatico(a);
                for (String nombre : nombres) {
                    if (!pendiente.equipos.contains(nombre)) {
                        pendiente.equipos.add(nombre);
                    }
                }
                movimientos.add(pendiente);
                pendiente = null;
            }
        }
        if (pendiente != null) {
            movimientos.add(pendiente);
        }
        // Solo el último movimiento puede seguir abierto: la persona sigue adentro hoy
        if (!movimientos.isEmpty()) {
            Mov ultimo = movimientos.get(movimientos.size() - 1);
            ultimo.abierto = ultimo.salida == null && ultimo.entrada != null && !ultimo.entrada.toLocalDate().isBefore(hoy);
        }
        return movimientos;
    }

    // El cierre de medianoche no tiene operador; una salida real a esa hora sí lo tiene
    private static boolean esCierreAutomatico(Acceso a) {
        return a.getOperadorId() == null && a.getFecha().toLocalTime().withNano(0).equals(HORA_CIERRE_AUTOMATICO);
    }

    private static List<Mov> recortar(List<Mov> movimientos, LocalDate inicio, LocalDate fin) {
        return movimientos.stream()
                .filter(m -> m.dias().stream().anyMatch(d -> !d.isBefore(inicio) && !d.isAfter(fin)))
                .toList();
    }

    // No se cuentan días futuros; por defecto solo lunes a viernes
    private static List<LocalDate> diasEsperados(LocalDate inicio, LocalDate fin, boolean soloHabiles) {
        List<LocalDate> dias = new ArrayList<>();
        for (LocalDate d = inicio; !d.isAfter(fin); d = d.plusDays(1)) {
            if (!soloHabiles || d.getDayOfWeek().getValue() <= DayOfWeek.FRIDAY.getValue()) {
                dias.add(d);
            }
        }
        return dias;
    }

    private static Resumen resumir(List<Mov> movimientos, List<LocalDate> esperados, List<String> motivos) {
        Set<LocalDate> conjuntoEsperados = Set.copyOf(esperados);
        Set<LocalDate> conMarca = new TreeSet<>();
        movimientos.forEach(m -> conMarca.addAll(m.dias()));

        long asistidos = conMarca.stream().filter(conjuntoEsperados::contains).count();
        long fueraDeComputo = conMarca.stream().filter(d -> !conjuntoEsperados.contains(d)).count();
        List<LocalDate> faltados = esperados.stream().filter(d -> !conMarca.contains(d)).toList();

        // Día más faltado por tasa (faltas / veces que ocurrió ese día), no por conteo bruto
        Map<String, int[]> porDia = new LinkedHashMap<>();
        esperados.forEach(d -> porDia.computeIfAbsent(DIAS[d.getDayOfWeek().getValue() - 1], k -> new int[2])[1]++);
        faltados.forEach(d -> porDia.get(DIAS[d.getDayOfWeek().getValue() - 1])[0]++);
        Map<String, Integer> faltasPorDia = new LinkedHashMap<>();
        Map<String, DetalleDia> detalle = new LinkedHashMap<>();
        porDia.forEach((dia, v) -> {
            if (v[0] > 0) faltasPorDia.put(dia, v[0]);
            detalle.put(dia, new DetalleDia(v[0], v[1]));
        });

        String diaPeor = null;
        List<String> empatados = List.of();
        String motivoSinDia;
        Map<String, int[]> validos = porDia.entrySet().stream()
                .filter(e -> e.getValue()[1] >= MINIMO_OPORTUNIDADES_DIA)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> x, LinkedHashMap::new));
        if (validos.isEmpty()) {
            motivoSinDia = "datos_insuficientes";
        } else {
            // Comparación exacta de fracciones (a/b vs c/d) para que los empates sean empates de verdad
            int[] mayor = validos.values().stream()
                    .reduce((x, y) -> (long) y[0] * x[1] > (long) x[0] * y[1] ? y : x).orElseThrow();
            if (mayor[0] == 0) {
                motivoSinDia = "sin_faltas";
            } else {
                List<String> iguales = validos.entrySet().stream()
                        .filter(e -> (long) e.getValue()[0] * mayor[1] == (long) mayor[0] * e.getValue()[1])
                        .map(Map.Entry::getKey).toList();
                if (iguales.size() > 1) {
                    empatados = iguales;
                    motivoSinDia = "empate";
                } else {
                    diaPeor = iguales.get(0);
                    motivoSinDia = null;
                }
            }
        }

        List<Long> minutos = movimientos.stream().map(Mov::permanencia).filter(m -> m != null).toList();
        Double porcentaje = esperados.isEmpty() ? null
                : BigDecimal.valueOf(asistidos * 100.0 / esperados.size()).setScale(1, RoundingMode.HALF_UP).doubleValue();
        return new Resumen(esperados.size(), (int) asistidos, faltados.size(), faltados, (int) fueraDeComputo,
                porcentaje, esperados.isEmpty() ? null : esperados.get(0),
                esperados.isEmpty() ? null : esperados.get(esperados.size() - 1), motivos, faltasPorDia, detalle, diaPeor,
                empatados, motivoSinDia, movimientos.size(),
                (int) movimientos.stream().filter(m -> m.salida == null && !m.abierto).count(),
                (int) movimientos.stream().filter(m -> m.abierto).count(),
                minutos.isEmpty() ? null : Math.round(minutos.stream().mapToLong(Long::longValue).average().orElse(0)));
    }

    private static Collection<Long> ids(List<Usuario> personas) {
        return personas.stream().map(Usuario::getId).toList();
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    // % y _ son comodines de SQL: se escapan para que un "%" escrito no traiga a todo el centro
    private static String patron(String busqueda) {
        if (busqueda.isEmpty()) {
            return "";
        }
        String escapado = busqueda.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escapado + "%";
    }

    private static final class Mov {
        private final LocalDateTime entrada;
        private LocalDateTime salida;
        private final List<String> equipos;
        private boolean cierreAutomatico;
        private final boolean entradaFueraDeVentana;
        private boolean abierto;

        Mov(LocalDateTime entrada, LocalDateTime salida, List<String> equipos, boolean cierreAutomatico,
            boolean entradaFueraDeVentana) {
            this.entrada = entrada;
            this.salida = salida;
            this.equipos = equipos;
            this.cierreAutomatico = cierreAutomatico;
            this.entradaFueraDeVentana = entradaFueraDeVentana;
        }

        // Cuenta el día de la entrada y el de la salida, no los intermedios: eso sería inventar presencia
        Set<LocalDate> dias() {
            Set<LocalDate> dias = new TreeSet<>();
            if (entrada != null) dias.add(entrada.toLocalDate());
            if (salida != null) dias.add(salida.toLocalDate());
            return dias;
        }

        Long permanencia() {
            return entrada != null && salida != null ? Duration.between(entrada, salida).toMinutes() : null;
        }

        // Los avisos explican un turno que cruza el borde del rango consultado, sin partirlo
        Movimiento aDto(LocalDate inicio, LocalDate fin) {
            LocalDate fecha = entrada != null ? entrada.toLocalDate() : salida.toLocalDate();
            return new Movimiento(fecha, entrada, salida, permanencia(), List.copyOf(equipos), abierto,
                    cierreAutomatico, entradaFueraDeVentana,
                    entrada != null && entrada.toLocalDate().isBefore(inicio),
                    salida != null && salida.toLocalDate().isAfter(fin));
        }
    }
}
