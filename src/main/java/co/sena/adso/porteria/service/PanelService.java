package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.PanelResponseDTO;
import co.sena.adso.porteria.dto.PanelResponseDTO.Grafica;
import co.sena.adso.porteria.dto.PanelResponseDTO.Indicadores;
import co.sena.adso.porteria.dto.RegistroAccesoDTO;
import co.sena.adso.porteria.dto.ReporteCargoResponseDTO;
import co.sena.adso.porteria.dto.ReporteCargoResponseDTO.Conteo;
import co.sena.adso.porteria.dto.ReporteCargoResponseDTO.PersonaAdentro;
import co.sena.adso.porteria.entity.Acceso;
import co.sena.adso.porteria.entity.ObjetoExterno;
import co.sena.adso.porteria.entity.Rol;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.entity.Vehiculo;
import co.sena.adso.porteria.entity.Visitante;
import co.sena.adso.porteria.exception.DatoInvalidoException;
import co.sena.adso.porteria.repository.AccesoRepository;
import co.sena.adso.porteria.repository.EquipoRepository;
import co.sena.adso.porteria.repository.ObjetoExternoRepository;
import co.sena.adso.porteria.repository.UsuarioRepository;
import co.sena.adso.porteria.repository.VehiculoRepository;
import co.sena.adso.porteria.repository.VisitanteRepository;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PanelService {

    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final int MAXIMO_DIAS_EXPORTACION = 366;
    // "Personal" agrupa a quien no es aprendiz ni instructor, repartido por cargo
    private static final String PERSONAL = "Personal";
    private static final List<String> CARGOS_FORMACION = List.of("Aprendiz", "Instructor");
    private static final Set<Character> INICIO_FORMULA = Set.of('=', '+', '-', '@', '\t', '\r');

    private final AccesoRepository accesoRepository;
    private final UsuarioRepository usuarioRepository;
    private final VisitanteRepository visitanteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ObjetoExternoRepository objetoRepository;
    private final EquipoRepository equipoRepository;
    private final AuthService authService;
    private final AuditoriaService auditoriaService;
    private final Clock reloj;

    public PanelService(AccesoRepository accesoRepository, UsuarioRepository usuarioRepository,
                        VisitanteRepository visitanteRepository, VehiculoRepository vehiculoRepository,
                        ObjetoExternoRepository objetoRepository, EquipoRepository equipoRepository,
                        AuthService authService, AuditoriaService auditoriaService, Clock reloj) {
        this.accesoRepository = accesoRepository;
        this.usuarioRepository = usuarioRepository;
        this.visitanteRepository = visitanteRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.objetoRepository = objetoRepository;
        this.equipoRepository = equipoRepository;
        this.authService = authService;
        this.auditoriaService = auditoriaService;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public PanelResponseDTO panel() {
        Indicadores indicadores = new Indicadores(
                usuarioRepository.countByCargo("Aprendiz"),
                usuarioRepository.countByCargo("Instructor"),
                usuarioRepository.countByRolNombre(Rol.TRABAJADOR),
                accesoRepository.contarAdentro(Acceso.VISITANTE),
                accesoRepository.contarAdentro(Acceso.VEHICULO),
                accesoRepository.contarAdentro(Acceso.OBJETO));

        LocalDate hoy = LocalDate.now(reloj);
        LocalDate primerDia = hoy.minusDays(6);
        List<String> dias = new ArrayList<>();
        Map<LocalDate, Integer> posicion = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate dia = primerDia.plusDays(i);
            posicion.put(dia, i);
            dias.add(dia.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES) + " " + dia.getDayOfMonth());
        }
        long[] aprendices = new long[7];
        long[] instructores = new long[7];
        long[] otros = new long[7];
        long total = 0;
        for (Object[] fila : accesoRepository.entradasPorDiaYCargo(primerDia.atStartOfDay())) {
            Integer i = posicion.get(((Date) fila[0]).toLocalDate());
            if (i == null) {
                continue;
            }
            long cantidad = ((Number) fila[2]).longValue();
            String cargo = (String) fila[1];
            if ("Aprendiz".equals(cargo)) aprendices[i] += cantidad;
            else if ("Instructor".equals(cargo)) instructores[i] += cantidad;
            else otros[i] += cantidad;
            total += cantidad;
        }
        String analisis = "Aún no hay suficientes datos para generar un análisis.";
        if (total > 0) {
            int mayor = 0;
            for (int i = 1; i < 7; i++) {
                if (aprendices[i] + instructores[i] + otros[i] > aprendices[mayor] + instructores[mayor] + otros[mayor]) {
                    mayor = i;
                }
            }
            analisis = "En los últimos 7 días se registraron " + total + " ingresos. El día con mayor actividad fue "
                    + dias.get(mayor) + " con " + (aprendices[mayor] + instructores[mayor] + otros[mayor]) + " ingresos.";
        }

        List<String> cargos = new ArrayList<>(Usuario.CARGOS_VALIDOS);
        cargos.addAll(List.of("Visitante", "Vehículo", "Objeto externo"));
        return new PanelResponseDTO(indicadores,
                new Grafica(dias, lista(aprendices), lista(instructores), lista(otros)),
                analisis, cargos, usuarioRepository.fichasEnUso());
    }

    /** Historial reciente del panel. Los filtros de cargo y ficha solo aplican a personas. */
    @Transactional(readOnly = true)
    public Page<RegistroAccesoDTO> recientes(LocalDate desde, LocalDate hasta, String cargo, String ficha, int pagina) {
        LocalDate fin = hasta == null ? LocalDate.now(reloj) : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(30) : desde;
        String tipoReferencia = "";
        String cargoFiltro = cargo == null ? "" : cargo;
        switch (cargoFiltro) {
            case "Visitante" -> { tipoReferencia = Acceso.VISITANTE; cargoFiltro = ""; }
            case "Vehículo" -> { tipoReferencia = Acceso.VEHICULO; cargoFiltro = ""; }
            case "Objeto externo" -> { tipoReferencia = Acceso.OBJETO; cargoFiltro = ""; }
            default -> { }
        }
        Page<Acceso> accesos = accesoRepository.buscarRecientes(inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay(),
                tipoReferencia, cargoFiltro, ficha == null ? "" : ficha, PageRequest.of(pagina, 25));
        Resolutor resolutor = new Resolutor(accesos.getContent());
        return accesos.map(resolutor::aDto);
    }

    /** CSV del histórico de personas. Exige rango de fechas y deja constancia en la auditoría. */
    @Transactional
    public String exportar(LocalDate desde, LocalDate hasta, String cargo, String ficha) {
        if (desde == null || hasta == null) {
            throw new DatoInvalidoException("Elige un rango de fechas (inicio y fin) antes de exportar");
        }
        if (hasta.isBefore(desde)) {
            throw new DatoInvalidoException("La fecha final debe ser igual o posterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) > MAXIMO_DIAS_EXPORTACION) {
            throw new DatoInvalidoException("El rango máximo de exportación es de un año");
        }
        List<Object[]> filas = accesoRepository.accesosDePersonas(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay(),
                cargo == null ? "" : cargo, ficha == null ? "" : ficha);

        Map<Long, String> equipos = equipoRepository.findAllById(filas.stream()
                        .flatMap(f -> ((Acceso) f[0]).listaEquipos().stream()).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(e -> e.getId(), e -> e.getNombre()));

        DateTimeFormatter fecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter hora = DateTimeFormatter.ofPattern("hh:mm:ss a", ES);
        StringBuilder csv = new StringBuilder("﻿");
        csv.append("Documento;Nombre;Cargo;Programa;Ficha;Equipos;Tipo;Fecha;Hora\n");
        for (Object[] fila : filas) {
            Acceso a = (Acceso) fila[0];
            Usuario u = (Usuario) fila[1];
            String nombresEquipos = a.listaEquipos().stream().map(id -> equipos.getOrDefault(id, "Equipo #" + id))
                    .collect(Collectors.joining(", "));
            csv.append(String.join(";", celda(u.getDocumento()), celda(u.getNombre()), celda(u.getCargo()),
                    celda(u.programaCarnet()), celda(u.numeroFicha()), celda(nombresEquipos.isEmpty() ? "Ninguno" : nombresEquipos),
                    celda(a.getTipo()), a.getFecha().format(fecha), a.getFecha().format(hora))).append('\n');
        }

        Usuario autor = authService.usuarioActual();
        auditoriaService.registrar(autor, "accesos", 0L, "Exportación de histórico de accesos", autor.getNombre(),
                "Exportación CSV desde el panel de portería",
                "Rango " + desde + " a " + hasta + (cargo == null ? "" : ", cargo " + cargo)
                        + (ficha == null ? "" : ", ficha " + ficha) + ". " + filas.size() + " registros exportados");
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public ReporteCargoResponseDTO reportePorCargo(String cargo) {
        List<String> cargos;
        if (PERSONAL.equals(cargo)) {
            cargos = Usuario.CARGOS_VALIDOS.stream().filter(c -> !CARGOS_FORMACION.contains(c)).toList();
        } else if (Usuario.CARGOS_VALIDOS.contains(cargo)) {
            cargos = List.of(cargo);
        } else {
            throw new DatoInvalidoException("Cargo no válido");
        }
        LocalDate hoy = LocalDate.now(reloj);
        List<Object[]> filas = accesoRepository.accesosDeCargos(hoy.minusDays(6).atStartOfDay(),
                hoy.plusDays(1).atStartOfDay(), cargos);
        Map<String, Long> hoyConteo = new HashMap<>();
        Map<String, Long> semana = new HashMap<>();
        for (Object[] fila : filas) {
            Acceso a = (Acceso) fila[0];
            if (!Acceso.ENTRADA.equals(a.getTipo())) {
                continue;
            }
            String grupo = grupoDe((Usuario) fila[1], cargo);
            semana.merge(grupo, 1L, Long::sum);
            if (a.getFecha().toLocalDate().equals(hoy)) {
                hoyConteo.merge(grupo, 1L, Long::sum);
            }
        }
        List<Conteo> conteoHoy = ordenar(hoyConteo);
        String analisis = conteoHoy.isEmpty()
                ? "Aún no hay ingresos de personas con cargo " + cargo + " el día de hoy."
                : "Hoy la mayoría de ingresos son de " + conteoHoy.get(0).grupo() + " con " + conteoHoy.get(0).total() + " registros.";

        List<Usuario> personas = usuarioRepository.findByCargoInOrderByNombre(cargos);
        List<PersonaAdentro> adentro = new ArrayList<>();
        if (!personas.isEmpty()) {
            Set<Long> ids = Set.copyOf(accesoRepository.quienesEstanAdentro(Acceso.USUARIO,
                    personas.stream().map(Usuario::getId).toList()));
            for (Usuario u : personas) {
                if (ids.contains(u.getId())) {
                    LocalDateTime ingreso = accesoRepository
                            .findFirstByReferenciaIdAndTipoReferenciaOrderByFechaDescIdDesc(u.getId(), Acceso.USUARIO)
                            .map(Acceso::getFecha).orElse(null);
                    adentro.add(new PersonaAdentro(u.getId(), u.getNombre(), u.getDocumento(),
                            PERSONAL.equals(cargo) ? u.getCargo() : u.programaCarnet(), u.numeroFicha(), ingreso));
                }
            }
        }
        return new ReporteCargoResponseDTO(cargo, conteoHoy, ordenar(semana), analisis, adentro);
    }

    private static String grupoDe(Usuario u, String cargo) {
        if (PERSONAL.equals(cargo)) {
            return u.getCargo() == null ? "Cargo no registrado" : u.getCargo();
        }
        if ("Aprendiz".equals(cargo)) {
            String programa = u.programaCarnet() == null ? "Sin programa" : u.programaCarnet();
            return programa + " (ficha " + (u.numeroFicha() == null ? "sin ficha" : u.numeroFicha()) + ")";
        }
        return u.programaCarnet() == null ? "Sin área registrada" : u.programaCarnet();
    }

    private static List<Conteo> ordenar(Map<String, Long> mapa) {
        return mapa.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new Conteo(e.getKey(), e.getValue()))
                .toList();
    }

    private static List<Long> lista(long[] valores) {
        List<Long> resultado = new ArrayList<>();
        for (long v : valores) resultado.add(v);
        return resultado;
    }

    // Excel ejecuta como fórmula lo que empieza por = + - @: se neutraliza con una comilla
    private static String celda(String valor) {
        String texto = valor == null ? "" : valor.replace(";", ",").replace("\n", " ").replace("\r", " ");
        if (!texto.isEmpty() && INICIO_FORMULA.contains(texto.charAt(0))) {
            return "'" + texto;
        }
        return texto;
    }

    /** Resuelve nombres y documentos de una página de accesos con una consulta por tipo, no una por fila. */
    private final class Resolutor {
        private final Map<Long, Usuario> usuarios;
        private final Map<Long, Visitante> visitantes;
        private final Map<Long, Vehiculo> vehiculos;
        private final Map<Long, ObjetoExterno> objetos;

        Resolutor(List<Acceso> accesos) {
            usuarios = porId(usuarioRepository.findAllById(ids(accesos, Acceso.USUARIO)), Usuario::getId);
            visitantes = porId(visitanteRepository.findAllById(ids(accesos, Acceso.VISITANTE)), Visitante::getId);
            vehiculos = porId(vehiculoRepository.findAllById(ids(accesos, Acceso.VEHICULO)), Vehiculo::getId);
            objetos = porId(objetoRepository.findAllById(ids(accesos, Acceso.OBJETO)), ObjetoExterno::getId);
        }

        RegistroAccesoDTO aDto(Acceso a) {
            Long ref = a.getReferenciaId();
            return switch (a.getTipoReferencia()) {
                case Acceso.USUARIO -> {
                    Usuario u = usuarios.get(ref);
                    yield fila(a, u == null ? "Usuario eliminado" : u.getNombre(), u == null ? null : u.getDocumento(),
                            u == null ? null : u.getCargo(),
                            u == null || u.programaCarnet() == null ? null
                                    : u.programaCarnet() + (u.numeroFicha() == null ? "" : " (ficha " + u.numeroFicha() + ")"),
                            u != null && u.tieneFotoPropia());
                }
                case Acceso.VISITANTE -> {
                    Visitante v = visitantes.get(ref);
                    yield fila(a, v == null ? "Visitante" : v.getNombre(), v == null ? null : v.getDocumento(),
                            "Visitante", v == null ? null : v.getMotivo(), false);
                }
                case Acceso.VEHICULO -> {
                    Vehiculo v = vehiculos.get(ref);
                    yield fila(a, v == null ? "Vehículo" : "Vehículo " + v.getPlaca(), v == null ? null : v.getPlaca(),
                            "Vehículo", v == null ? null : v.getMotivo(), false);
                }
                default -> {
                    ObjetoExterno o = objetos.get(ref);
                    yield fila(a, o == null ? "Objeto externo" : o.getDescripcion(), o == null ? null : o.getSerial(),
                            "Objeto externo", o == null ? null : o.getMotivo(), false);
                }
            };
        }

        private RegistroAccesoDTO fila(Acceso a, String nombre, String documento, String clase, String detalle, boolean foto) {
            return new RegistroAccesoDTO(a.getId(), a.getTipo(), a.getFecha(), a.getTipoReferencia(), a.getReferenciaId(),
                    nombre, documento, clase, detalle, foto);
        }

        private List<Long> ids(List<Acceso> accesos, String tipo) {
            return accesos.stream().filter(a -> tipo.equals(a.getTipoReferencia()))
                    .map(Acceso::getReferenciaId).distinct().toList();
        }

        private <T> Map<Long, T> porId(List<T> lista, Function<T, Long> id) {
            return lista.stream().collect(Collectors.toMap(id, Function.identity(), (x, y) -> x, LinkedHashMap::new));
        }
    }
}
