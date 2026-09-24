package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.EquipoResponseDTO;
import co.sena.adso.porteria.dto.IncidenteRequestDTO;
import co.sena.adso.porteria.dto.MensajeResponseDTO;
import co.sena.adso.porteria.dto.MovimientoRequestDTO;
import co.sena.adso.porteria.dto.VerificacionResponseDTO;
import co.sena.adso.porteria.entity.Acceso;
import co.sena.adso.porteria.entity.Equipo;
import co.sena.adso.porteria.entity.ObjetoExterno;
import co.sena.adso.porteria.entity.PuntoAcceso;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.entity.Vehiculo;
import co.sena.adso.porteria.entity.Visitante;
import co.sena.adso.porteria.exception.ConflictoException;
import co.sena.adso.porteria.exception.ResourceNotFoundException;
import co.sena.adso.porteria.repository.AccesoRepository;
import co.sena.adso.porteria.repository.EquipoRepository;
import co.sena.adso.porteria.repository.ObjetoExternoRepository;
import co.sena.adso.porteria.repository.PuntoAccesoRepository;
import co.sena.adso.porteria.repository.UsuarioRepository;
import co.sena.adso.porteria.repository.VehiculoRepository;
import co.sena.adso.porteria.repository.VisitanteRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PorteriaService {

    // Un visitante que lleva más de esto adentro se resalta en el escáner
    private static final Duration LIMITE_VISITA = Duration.ofHours(2);

    private final UsuarioRepository usuarioRepository;
    private final VisitanteRepository visitanteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ObjetoExternoRepository objetoRepository;
    private final EquipoRepository equipoRepository;
    private final AccesoRepository accesoRepository;
    private final PuntoAccesoRepository puntoRepository;
    private final AuthService authService;
    private final AuditoriaService auditoriaService;
    private final Clock reloj;

    public PorteriaService(UsuarioRepository usuarioRepository, VisitanteRepository visitanteRepository,
                           VehiculoRepository vehiculoRepository, ObjetoExternoRepository objetoRepository,
                           EquipoRepository equipoRepository, AccesoRepository accesoRepository,
                           PuntoAccesoRepository puntoRepository, AuthService authService,
                           AuditoriaService auditoriaService, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.visitanteRepository = visitanteRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.objetoRepository = objetoRepository;
        this.equipoRepository = equipoRepository;
        this.accesoRepository = accesoRepository;
        this.puntoRepository = puntoRepository;
        this.authService = authService;
        this.auditoriaService = auditoriaService;
        this.reloj = reloj;
    }

    /** Identifica lo escaneado: carnet (documento), pase de visitante, vehículo u objeto externo. */
    @Transactional(readOnly = true)
    public VerificacionResponseDTO verificar(String codigoLeido) {
        String codigo = codigoLeido.trim();
        if (codigo.startsWith(Visitante.PREFIJO)) {
            return visitanteRepository.findByDocumento(sinPrefijo(codigo, Visitante.PREFIJO))
                    .map(this::verificarVisitante).orElse(VerificacionResponseDTO.noEncontrado());
        }
        if (codigo.startsWith(Vehiculo.PREFIJO_SENA) || codigo.startsWith(Vehiculo.PREFIJO_EXTERNO)) {
            String placa = codigo.substring(codigo.indexOf(':') + 1).trim().toUpperCase();
            return vehiculoRepository.findByPlaca(placa)
                    .map(v -> entidad(Acceso.VEHICULO, v.getId(), "Vehículo " + v.getPlaca(), v.getPlaca(),
                            v.getTipo(), "Logística"))
                    .orElse(VerificacionResponseDTO.noEncontrado());
        }
        if (codigo.startsWith(ObjetoExterno.PREFIJO)) {
            return objetoRepository.findBySerial(sinPrefijo(codigo, ObjetoExterno.PREFIJO))
                    .map(o -> entidad(Acceso.OBJETO, o.getId(), o.getDescripcion(), o.getSerial(),
                            o.getPropietario() == null ? "Externo" : o.getPropietario(), "Equipo de tercero"))
                    .orElse(VerificacionResponseDTO.noEncontrado());
        }
        String documento = codigo.startsWith("SENA-CARNET:") ? sinPrefijo(codigo, "SENA-CARNET:") : codigo;
        return usuarioRepository.findByDocumento(documento)
                .map(this::verificarUsuario).orElse(VerificacionResponseDTO.noEncontrado());
    }

    // noRollbackFor: la inconsistencia se rechaza, pero su registro en la auditoría debe quedar guardado
    @Transactional(noRollbackFor = ConflictoException.class)
    public MensajeResponseDTO registrarMovimiento(MovimientoRequestDTO datos) {
        Usuario operador = authService.usuarioActual();
        String tipoEntidad = datos.tipoEntidad();
        Long id = datos.entidadId();
        validarQueExiste(tipoEntidad, id);

        String estado = estadoActual(id, tipoEntidad);
        String inconsistencia = inconsistencia(tipoEntidad, id, datos.tipo(), estado);
        if (inconsistencia != null) {
            auditoriaService.registrar(operador, "accesos", id, "Inconsistencia de acceso detectada",
                    operador.getNombre(), "Registro de movimiento con inconsistencia de estado", inconsistencia);
            throw new ConflictoException(inconsistencia);
        }

        // Solo se aceptan equipos que sean de esa persona
        List<Equipo> equipos = List.of();
        if (Acceso.USUARIO.equals(tipoEntidad) && datos.equiposIds() != null && !datos.equiposIds().isEmpty()) {
            equipos = equipoRepository.findByUsuarioIdAndIdIn(id, datos.equiposIds());
            String nuevoEstado = Acceso.ENTRADA.equals(datos.tipo()) ? Equipo.ADENTRO : Equipo.AFUERA;
            equipos.forEach(e -> e.setEstado(nuevoEstado));
        }
        String equiposIds = equipos.isEmpty() ? null
                : equipos.stream().map(e -> String.valueOf(e.getId())).collect(Collectors.joining(","));

        accesoRepository.save(new Acceso(puntoPrincipal(), id, tipoEntidad, datos.tipo(),
                LocalDateTime.now(reloj), equiposIds, operador.getId()));
        return new MensajeResponseDTO(datos.tipo() + " registrada correctamente");
    }

    @Transactional
    public MensajeResponseDTO registrarIncidente(IncidenteRequestDTO datos) {
        Usuario operador = authService.usuarioActual();
        String tipo = datos.tipoEntidad() == null ? "Desconocido" : datos.tipoEntidad();
        auditoriaService.registrar(operador, tipo.toLowerCase(), datos.entidadId() == null ? 0L : datos.entidadId(),
                "Incidente registrado en portería", operador.getNombre(),
                "Reporte de anomalía o equipo no registrado", datos.detalles().trim());
        return new MensajeResponseDTO("Incidente registrado");
    }

    public String estadoActual(Long referenciaId, String tipoReferencia) {
        return accesoRepository.findFirstByReferenciaIdAndTipoReferenciaOrderByFechaDescIdDesc(referenciaId, tipoReferencia)
                .map(Acceso::getTipo).orElse(Acceso.AFUERA);
    }

    PuntoAcceso puntoPrincipal() {
        return puntoRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay puntos de acceso configurados"));
    }

    private VerificacionResponseDTO verificarUsuario(Usuario u) {
        List<EquipoResponseDTO> equipos = equipoRepository.findByUsuarioIdOrderById(u.getId()).stream()
                .map(EquipoResponseDTO::fromEntity).toList();
        // El celador debe saber si la foto ya fue verificada: una foto sin aprobar no confirma identidad
        return new VerificacionResponseDTO(true, Acceso.USUARIO, u.getId(), u.getNombre(), u.getDocumento(),
                u.getCargo(), u.getRol().getNombre(), estadoActual(u.getId(), Acceso.USUARIO), u.tieneFotoPropia(),
                Usuario.FOTO_APROBADA.equals(u.getFotoEstado()), u.isPerfilCompleto(), equipos, null, null);
    }

    private VerificacionResponseDTO verificarVisitante(Visitante v) {
        var ultimo = accesoRepository.findFirstByReferenciaIdAndTipoReferenciaOrderByFechaDescIdDesc(v.getId(), Acceso.VISITANTE);
        String estado = ultimo.map(Acceso::getTipo).orElse(Acceso.AFUERA);
        String tiempo = null;
        Boolean excedido = null;
        if (ultimo.isPresent() && Acceso.ENTRADA.equals(estado)) {
            Duration adentro = Duration.between(ultimo.get().getFecha(), LocalDateTime.now(reloj));
            if (!adentro.isNegative()) {
                tiempo = adentro.toHours() + "h " + adentro.toMinutesPart() + "m";
                excedido = adentro.compareTo(LIMITE_VISITA) > 0;
            }
        }
        return new VerificacionResponseDTO(true, Acceso.VISITANTE, v.getId(), v.getNombre(), v.getDocumento(),
                "Visitante", "Externo", estado, null, null, null, null, tiempo, excedido);
    }

    private VerificacionResponseDTO entidad(String tipo, Long id, String nombre, String documento,
                                            String cargo, String rol) {
        return new VerificacionResponseDTO(true, tipo, id, nombre, documento, cargo, rol, estadoActual(id, tipo),
                null, null, null, null, null, null);
    }

    private void validarQueExiste(String tipo, Long id) {
        boolean existe = switch (tipo) {
            case Acceso.USUARIO -> usuarioRepository.existsById(id);
            case Acceso.VISITANTE -> visitanteRepository.existsById(id);
            case Acceso.VEHICULO -> vehiculoRepository.existsById(id);
            case Acceso.OBJETO -> objetoRepository.existsById(id);
            default -> false;
        };
        if (!existe) {
            throw new ResourceNotFoundException("un registro de tipo " + tipo, id);
        }
    }

    // Dos entradas seguidas o una salida sin entrada no se registran: quedan en la auditoría
    private static String inconsistencia(String tipo, Long id, String movimiento, String estado) {
        if (Acceso.SALIDA.equals(movimiento) && !Acceso.ENTRADA.equals(estado)) {
            return "Se intentó registrar la salida de " + tipo + " " + id + " sin una entrada previa";
        }
        if (Acceso.ENTRADA.equals(movimiento) && Acceso.ENTRADA.equals(estado)) {
            return "Se intentó registrar la entrada de " + tipo + " " + id + ", que ya figura adentro";
        }
        return null;
    }

    private static String sinPrefijo(String codigo, String prefijo) {
        return codigo.substring(prefijo.length()).trim();
    }
}
