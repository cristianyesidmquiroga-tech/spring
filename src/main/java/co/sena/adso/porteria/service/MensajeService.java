package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.AvisosResponseDTO;
import co.sena.adso.porteria.dto.ConversacionResponseDTO;
import co.sena.adso.porteria.dto.HiloResponseDTO;
import co.sena.adso.porteria.dto.MensajeResponseDTO;
import co.sena.adso.porteria.entity.Mensaje;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.exception.DatoInvalidoException;
import co.sena.adso.porteria.exception.ResourceNotFoundException;
import co.sena.adso.porteria.repository.MensajeRepository;
import co.sena.adso.porteria.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Canal entre cada persona y los asesores, para quien queda bloqueado sin carnet
@Service
public class MensajeService {

    private final MensajeRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthService authService;
    private final RespaldoService respaldoService;
    private final Clock reloj;

    public MensajeService(MensajeRepository mensajeRepository, UsuarioRepository usuarioRepository,
                          AuthService authService, RespaldoService respaldoService, Clock reloj) {
        this.respaldoService = respaldoService;
        this.mensajeRepository = mensajeRepository;
        this.usuarioRepository = usuarioRepository;
        this.authService = authService;
        this.reloj = reloj;
    }

    // Abrir el hilo da por leído lo que escribieron los asesores
    @Transactional
    public ConversacionResponseDTO misMensajes() {
        Usuario yo = authService.usuarioActual();
        mensajeRepository.marcarLeidosPorLaPersona(yo.getId());
        return conversacion(yo, yo.getDocumento(), yo.getId());
    }

    @Transactional
    public MensajeResponseDTO enviar(String texto) {
        Usuario yo = authService.usuarioActual();
        registrar(yo.getId(), yo, limpio(texto), false);
        return new MensajeResponseDTO("Mensaje enviado. Un administrador te responderá por aquí.");
    }

    // Primero quien espera respuesta, y dentro de eso lo más reciente
    @Transactional(readOnly = true)
    public List<HiloResponseDTO> bandeja() {
        List<Object[]> resumen = mensajeRepository.resumenDeHilos();
        List<Long> ids = resumen.stream().map(f -> (Long) f[0]).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Usuario> personas = usuarioRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));
        Map<Long, Mensaje> ultimos = mensajeRepository.ultimosDe(ids).stream()
                .collect(Collectors.toMap(Mensaje::getUsuarioId, Function.identity()));
        return resumen.stream().filter(f -> personas.containsKey((Long) f[0])).map(f -> {
                    Usuario p = personas.get((Long) f[0]);
                    Mensaje ultimo = ultimos.get(p.getId());
                    return new HiloResponseDTO(p.getId(), p.getNombre(), p.getCargo(), enmascarar(p.getDocumento()),
                            p.tieneFotoPropia(), ultimo.getTexto(), ultimo.isAutorEsAdmin(), (LocalDateTime) f[1],
                            ((Number) f[2]).longValue());
                })
                .sorted(Comparator.comparingLong(HiloResponseDTO::sinLeer).reversed()
                        .thenComparing(HiloResponseDTO::ultimaFecha, Comparator.reverseOrder()))
                .toList();
    }

    @Transactional
    public ConversacionResponseDTO hilo(Long usuarioId) {
        Usuario persona = buscar(usuarioId);
        mensajeRepository.marcarLeidosPorElAsesor(usuarioId);
        return conversacion(persona, enmascarar(persona.getDocumento()), authService.usuarioActual().getId());
    }

    @Transactional
    public MensajeResponseDTO responder(Long usuarioId, String texto) {
        Usuario persona = buscar(usuarioId);
        registrar(persona.getId(), authService.usuarioActual(), limpio(texto), false);
        return new MensajeResponseDTO("Mensaje enviado a " + persona.getNombre() + ".");
    }

    // No confirma la transacción: la decide quien llama, junto con el resto de la operación
    public void registrar(Long usuarioId, Usuario autor, String texto, boolean automatico) {
        mensajeRepository.save(new Mensaje(usuarioId, autor, texto, automatico, LocalDateTime.now(reloj)));
    }

    @Transactional(readOnly = true)
    public AvisosResponseDTO avisos() {
        Usuario yo = authService.usuarioActual();
        long pendientes = yo.puedeAsesorar() ? mensajeRepository.hilosConRespuestaPendiente() : 0;
        long fotos = yo.esAdmin() ? usuarioRepository.countByFotoEstado(Usuario.FOTO_PENDIENTE) : 0;
        return new AvisosResponseDTO(mensajeRepository.sinLeerParaLaPersona(yo.getId()), pendientes, fotos,
                yo.esAdmin() ? respaldoService.aviso() : null);
    }

    static String limpio(String texto) {
        String limpio = Texto.opcional(texto);
        if (limpio == null) {
            throw new DatoInvalidoException("Escribe un mensaje antes de enviarlo.");
        }
        return limpio;
    }

    // Quien asesora no necesita la cédula completa (minimización, Ley 1581 art. 4 lit. c)
    static String enmascarar(String documento) {
        if (documento == null || documento.isBlank()) {
            return "N/A";
        }
        if (documento.length() <= 4) {
            return "*".repeat(documento.length());
        }
        return "*".repeat(documento.length() - 4) + documento.substring(documento.length() - 4);
    }

    private ConversacionResponseDTO conversacion(Usuario persona, String documento, Long quienMira) {
        List<ConversacionResponseDTO.Item> items = mensajeRepository.findByUsuarioIdOrderByFechaAscIdAsc(persona.getId())
                .stream().map(m -> ConversacionResponseDTO.Item.fromEntity(m, quienMira)).toList();
        return new ConversacionResponseDTO(new ConversacionResponseDTO.Persona(persona.getId(), persona.getNombre(),
                persona.getCargo(), documento, persona.tieneFotoPropia(), persona.getFotoEstado()), items);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("un usuario", id));
    }
}
