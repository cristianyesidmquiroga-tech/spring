package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.EquipoRequestDTO;
import co.sena.adso.porteria.dto.EquipoResponseDTO;
import co.sena.adso.porteria.entity.Equipo;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.exception.BusinessException;
import co.sena.adso.porteria.exception.DatoInvalidoException;
import co.sena.adso.porteria.exception.ResourceNotFoundException;
import co.sena.adso.porteria.repository.EquipoRepository;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipoService {

    private final EquipoRepository equipoRepository;
    private final AuthService authService;

    public EquipoService(EquipoRepository equipoRepository, AuthService authService) {
        this.equipoRepository = equipoRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<EquipoResponseDTO> misEquipos() {
        return equipoRepository.findByUsuarioIdOrderById(authService.usuarioActual().getId()).stream()
                .map(EquipoResponseDTO::fromEntity).toList();
    }

    @Transactional
    public EquipoResponseDTO registrar(EquipoRequestDTO datos) {
        Usuario usuario = authService.usuarioActual();
        if (!usuario.puedeRegistrarEquipos()) {
            throw new AccessDeniedException("Tu perfil no puede registrar equipos");
        }
        if (!Equipo.TIPOS.contains(datos.tipo())) {
            throw new DatoInvalidoException("Tipo de equipo no válido");
        }
        if (equipoRepository.countByUsuarioId(usuario.getId()) >= Equipo.MAXIMO_POR_USUARIO) {
            throw new BusinessException("Solo puedes registrar hasta " + Equipo.MAXIMO_POR_USUARIO + " equipos");
        }
        String serial = datos.serial() == null || datos.serial().isBlank() ? null : datos.serial().trim().toUpperCase();
        if (serial != null && equipoRepository.existsBySerial(serial)) {
            throw new BusinessException("Ya existe un equipo registrado con ese serial");
        }
        Equipo equipo = equipoRepository.save(new Equipo(datos.nombre().trim(), serial, datos.tipo(), usuario.getId()));
        return EquipoResponseDTO.fromEntity(equipo);
    }

    @Transactional
    public void eliminar(Long id) {
        Equipo equipo = equipoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("un equipo", id));
        // Cada quien solo borra sus propios equipos
        if (!equipo.getUsuarioId().equals(authService.usuarioActual().getId())) {
            throw new AccessDeniedException("El equipo no es tuyo");
        }
        equipoRepository.delete(equipo);
    }
}
