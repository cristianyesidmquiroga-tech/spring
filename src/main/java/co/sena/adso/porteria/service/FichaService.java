package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.FichaRequestDTO;
import co.sena.adso.porteria.dto.FichaResponseDTO;
import co.sena.adso.porteria.entity.Ficha;
import co.sena.adso.porteria.exception.ConflictoException;
import co.sena.adso.porteria.exception.ResourceNotFoundException;
import co.sena.adso.porteria.repository.FichaRepository;
import co.sena.adso.porteria.repository.UsuarioRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Editar una ficha cambia de golpe el carnet de todos sus aprendices, por eso solo la maneja el admin
@Service
public class FichaService {

    private final FichaRepository fichaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final AuthService authService;

    public FichaService(FichaRepository fichaRepository, UsuarioRepository usuarioRepository,
                        AuditoriaService auditoriaService, AuthService authService) {
        this.fichaRepository = fichaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<FichaResponseDTO> listar() {
        Map<Long, Long> conteo = usuarioRepository.contarPorFicha().stream()
                .collect(Collectors.toMap(f -> (Long) f[0], f -> (Long) f[1]));
        return fichaRepository.findAllByOrderByActivaDescNumeroAsc().stream()
                .map(f -> FichaResponseDTO.fromEntity(f, conteo.getOrDefault(f.getId(), 0L)))
                .toList();
    }

    @Transactional
    public FichaResponseDTO crear(FichaRequestDTO datos) {
        String numero = datos.numero().trim();
        if (fichaRepository.existsByNumero(numero)) {
            throw new ConflictoException("La ficha " + numero + " ya está registrada.");
        }
        Ficha ficha = fichaRepository.save(new Ficha(numero, Texto.limpiar(datos.programa()), datos.fechaFinalizacion()));
        auditoriaService.registrar(authService.usuarioActual(), "fichas", ficha.getId(), "Creó la ficha " + numero,
                null, null, null);
        return FichaResponseDTO.fromEntity(ficha, 0);
    }

    @Transactional
    public FichaResponseDTO editar(Long id, FichaRequestDTO datos) {
        Ficha ficha = buscar(id);
        String numero = datos.numero().trim();
        if (fichaRepository.existsByNumeroAndIdNot(numero, id)) {
            throw new ConflictoException("Ya existe otra ficha con el número " + numero + ".");
        }
        ficha.actualizar(numero, Texto.limpiar(datos.programa()), datos.fechaFinalizacion());
        usuarioRepository.sincronizarFicha(id, ficha.getNumero(), ficha.getPrograma());
        auditoriaService.registrar(authService.usuarioActual(), "fichas", id, "Editó la ficha " + numero,
                null, null, null);
        return FichaResponseDTO.fromEntity(ficha, contar(id));
    }

    @Transactional
    public FichaResponseDTO alternarArchivo(Long id) {
        Ficha ficha = buscar(id);
        ficha.alternarActiva();
        auditoriaService.registrar(authService.usuarioActual(), "fichas", id,
                (ficha.isActiva() ? "Reactivó" : "Archivó") + " la ficha " + ficha.getNumero(), null, null, null);
        return FichaResponseDTO.fromEntity(ficha, contar(id));
    }

    private Ficha buscar(Long id) {
        return fichaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("una ficha", id));
    }

    private long contar(Long id) {
        return usuarioRepository.contarPorFicha().stream()
                .filter(f -> id.equals(f[0])).mapToLong(f -> (Long) f[1]).findFirst().orElse(0);
    }
}
