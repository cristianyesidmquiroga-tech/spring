package co.sena.adso.fincasapi.service;

import co.sena.adso.fincasapi.dto.CultivoRequestDTO;
import co.sena.adso.fincasapi.dto.CultivoResponseDTO;
import co.sena.adso.fincasapi.entity.Cultivo;
import co.sena.adso.fincasapi.exception.ResourceNotFoundException;
import co.sena.adso.fincasapi.exception.BusinessException;
import co.sena.adso.fincasapi.repository.CultivoRepository;
import co.sena.adso.fincasapi.repository.FincaCultivoRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CultivoService {

    private final CultivoRepository cultivoRepository;
    private final FincaCultivoRepository siembraRepository;

    public CultivoService(CultivoRepository cultivoRepository, FincaCultivoRepository siembraRepository) {
        this.cultivoRepository = cultivoRepository;
        this.siembraRepository = siembraRepository;
    }

    @Transactional(readOnly = true)
    public List<CultivoResponseDTO> listar() {
        return cultivoRepository.findAll(Sort.by("nombre")).stream()
                .map(CultivoResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CultivoResponseDTO obtener(Long id) {
        return CultivoResponseDTO.fromEntity(buscarCultivo(id));
    }

    @Transactional
    public CultivoResponseDTO crear(CultivoRequestDTO datos) {
        String nombre = datos.nombre().trim();
        if (cultivoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe un cultivo llamado " + nombre);
        }
        Cultivo cultivo = new Cultivo(nombre, datos.tipo(), datos.cicloDias());
        return CultivoResponseDTO.fromEntity(cultivoRepository.save(cultivo));
    }

    @Transactional
    public CultivoResponseDTO actualizar(Long id, CultivoRequestDTO datos) {
        Cultivo cultivo = buscarCultivo(id);
        String nombre = datos.nombre().trim();
        if (cultivoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new BusinessException("Ya existe otro cultivo llamado " + nombre);
        }
        cultivo.actualizar(nombre, datos.tipo(), datos.cicloDias());
        return CultivoResponseDTO.fromEntity(cultivo);
    }

    @Transactional
    public void eliminar(Long id) {
        Cultivo cultivo = buscarCultivo(id);
        if (siembraRepository.existsByCultivoId(id)) {
            throw new BusinessException("El cultivo " + cultivo.getNombre()
                    + " tiene siembras registradas; elimínalas primero");
        }
        cultivoRepository.delete(cultivo);
    }

    Cultivo buscarCultivo(Long id) {
        return cultivoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("un cultivo", id));
    }
}
