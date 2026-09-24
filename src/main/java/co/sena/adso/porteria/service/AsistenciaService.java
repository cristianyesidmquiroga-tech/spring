package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.AmbienteResponseDTO;
import co.sena.adso.porteria.dto.AsistenciaRequestDTO;
import co.sena.adso.porteria.dto.AsistenciaResponseDTO;
import co.sena.adso.porteria.dto.ClaseResponseDTO;
import co.sena.adso.porteria.dto.DetalleAmbienteResponseDTO;
import co.sena.adso.porteria.dto.MensajeResponseDTO;
import co.sena.adso.porteria.entity.AsistenciaClase;
import co.sena.adso.porteria.entity.Ficha;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.repository.AsistenciaClaseRepository;
import co.sena.adso.porteria.repository.FichaRepository;
import co.sena.adso.porteria.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Lista de clase del instructor, ambientes para coordinación y el historial para el admin
@Service
public class AsistenciaService {

    private static final int LIMITE_HISTORIAL = 100;
    private static final String SIN_PROGRAMA = "Sin programa";

    private final UsuarioRepository usuarioRepository;
    private final AsistenciaClaseRepository asistenciaRepository;
    private final FichaRepository fichaRepository;
    private final AuthService authService;
    private final Clock reloj;

    public AsistenciaService(UsuarioRepository usuarioRepository, AsistenciaClaseRepository asistenciaRepository,
                             FichaRepository fichaRepository, AuthService authService, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.fichaRepository = fichaRepository;
        this.authService = authService;
        this.reloj = reloj;
    }

    // Solo salen quienes ya cruzaron portería hoy
    @Transactional(readOnly = true)
    public AsistenciaResponseDTO buscar(String ficha) {
        String numero = ficha == null ? "" : ficha.trim();
        if (numero.isEmpty()) {
            return new AsistenciaResponseDTO("", List.of());
        }
        List<AsistenciaResponseDTO.Aprendiz> aprendices = llegadasDeHoy(numero).entrySet().stream()
                .map(e -> AsistenciaResponseDTO.Aprendiz.fromEntity(e.getKey(), e.getValue()))
                .toList();
        return new AsistenciaResponseDTO(numero, aprendices);
    }

    // Guardar otra vez el mismo día reemplaza la lista anterior en vez de contar faltas dobles
    @Transactional
    public MensajeResponseDTO guardar(AsistenciaRequestDTO datos) {
        String ficha = datos.ficha().trim();
        Usuario instructor = authService.usuarioActual();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDateTime inicio = ahora.toLocalDate().atStartOfDay();
        Set<Long> presentes = new HashSet<>(datos.presentes());

        asistenciaRepository.borrarDeFichaEnRango(ficha, inicio, inicio.plusDays(1));
        List<AsistenciaClase> lista = llegadasDeHoy(ficha).keySet().stream()
                .map(a -> new AsistenciaClase(instructor, a, ficha, ahora, presentes.contains(a.getId())))
                .toList();
        asistenciaRepository.saveAll(lista);
        return new MensajeResponseDTO("Reporte guardado exitosamente.");
    }

    @Transactional(readOnly = true)
    public List<AmbienteResponseDTO> ambientes() {
        LocalDateTime inicio = hoy();
        List<Object[]> filas = usuarioRepository.fichasConAprendicesAdentro(inicio, inicio.plusDays(1));
        List<String> numeros = filas.stream().map(f -> (String) f[0]).toList();
        Map<String, Ficha> fichas = fichaRepository.findByNumeroIn(numeros).stream()
                .collect(Collectors.toMap(Ficha::getNumero, Function.identity()));
        Map<String, AsistenciaClase> primeraLista = new LinkedHashMap<>();
        if (!numeros.isEmpty()) {
            asistenciaRepository.deFichasEnRango(numeros, inicio, inicio.plusDays(1))
                    .forEach(a -> primeraLista.putIfAbsent(a.getFicha(), a));
        }
        return filas.stream().map(f -> {
            String numero = (String) f[0];
            AsistenciaClase lista = primeraLista.get(numero);
            Usuario instructor = lista != null ? lista.getInstructor() : null;
            return new AmbienteResponseDTO(numero, programa(fichas.get(numero), (String) f[2]), (Long) f[1],
                    instructor != null ? instructor.getNombre() : "Sin instructor registrado", lista != null);
        }).toList();
    }

    @Transactional(readOnly = true)
    public DetalleAmbienteResponseDTO detalleAmbiente(String ficha) {
        String numero = ficha.trim();
        LocalDateTime inicio = hoy();
        Map<Usuario, LocalDateTime> llegadas = llegadasDeHoy(numero);
        Map<Long, AsistenciaClase> asistencias = new LinkedHashMap<>();
        asistenciaRepository.deFichasEnRango(List.of(numero), inicio, inicio.plusDays(1))
                .forEach(a -> asistencias.put(a.getAprendiz().getId(), a));

        Usuario instructor = asistencias.values().stream().map(AsistenciaClase::getInstructor)
                .filter(i -> i != null).findFirst().orElse(null);
        String textoPrograma = llegadas.keySet().stream().map(Usuario::getPrograma)
                .filter(p -> p != null && !p.isBlank()).findFirst().orElse(null);

        List<DetalleAmbienteResponseDTO.Aprendiz> aprendices = llegadas.entrySet().stream().map(e -> {
            Usuario u = e.getKey();
            AsistenciaClase a = asistencias.get(u.getId());
            return new DetalleAmbienteResponseDTO.Aprendiz(u.getId(), u.getNombre(), u.getDocumento(), u.getCargo(),
                    u.tieneFotoPropia(), e.getValue(), a != null ? a.isPresente() : null,
                    a != null ? a.getEvaluacion() : null);
        }).toList();

        DetalleAmbienteResponseDTO.Instructor datosInstructor = instructor == null ? null
                : new DetalleAmbienteResponseDTO.Instructor(instructor.getId(), instructor.getNombre(),
                instructor.getCargo(), instructor.getPrograma(), instructor.tieneFotoPropia());
        return new DetalleAmbienteResponseDTO(numero, programa(fichaRepository.findByNumero(numero).orElse(null),
                textoPrograma), datosInstructor, aprendices);
    }

    @Transactional(readOnly = true)
    public List<ClaseResponseDTO> historialClases(String ficha) {
        String numero = ficha == null ? "" : ficha.trim();
        if (numero.isEmpty()) {
            return List.of();
        }
        return asistenciaRepository.historialDeFicha(numero, PageRequest.of(0, LIMITE_HISTORIAL)).stream()
                .map(ClaseResponseDTO::fromEntity).toList();
    }

    private Map<Usuario, LocalDateTime> llegadasDeHoy(String ficha) {
        LocalDateTime inicio = hoy();
        Map<Usuario, LocalDateTime> llegadas = new LinkedHashMap<>();
        usuarioRepository.llegadasDeFicha(ficha, inicio, inicio.plusDays(1))
                .forEach(f -> llegadas.put((Usuario) f[0], (LocalDateTime) f[1]));
        return llegadas;
    }

    private LocalDateTime hoy() {
        return LocalDate.now(reloj).atStartOfDay();
    }

    private static String programa(Ficha ficha, String texto) {
        if (ficha != null) {
            return ficha.getPrograma();
        }
        return texto != null && !texto.isBlank() ? texto : SIN_PROGRAMA;
    }
}
