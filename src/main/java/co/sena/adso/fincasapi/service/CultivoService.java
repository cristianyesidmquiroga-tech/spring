package co.sena.adso.fincasapi.service;

import co.sena.adso.fincasapi.dto.CultivoRequestDTO;
import co.sena.adso.fincasapi.dto.CultivoResponseDTO;
import co.sena.adso.fincasapi.entity.Cultivo;
import co.sena.adso.fincasapi.exception.ResourceNotFoundException;
import co.sena.adso.fincasapi.repository.CultivoRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CultivoService {

    private final CultivoRepository cultivoRepository;

    public CultivoService(CultivoRepository cultivoRepository) {
        this.cultivoRepository = cultivoRepository;
    }

    @Transactional(readOnly = true)
    public List<CultivoResponseDTO> listar() {
        return cultivoRepository.findAll(Sort.by("id")).stream()
                .map(CultivoResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CultivoResponseDTO obtener(Long id) {
        return CultivoResponseDTO.fromEntity(buscarCultivo(id));
    }

    @Transactional
    public CultivoResponseDTO crear(CultivoRequestDTO datos) {
        Cultivo cultivo = new Cultivo(datos.nombre().trim(), datos.tipo().trim(), datos.cicloDias());
        return CultivoResponseDTO.fromEntity(cultivoRepository.save(cultivo));
    }

    @Transactional
    public CultivoResponseDTO actualizar(Long id, CultivoRequestDTO datos) {
        Cultivo cultivo = buscarCultivo(id);
        cultivo.actualizar(datos.nombre().trim(), datos.tipo().trim(), datos.cicloDias());
        return CultivoResponseDTO.fromEntity(cultivo);
    }

    @Transactional
    public void eliminar(Long id) {
        cultivoRepository.delete(buscarCultivo(id));
    }

    Cultivo buscarCultivo(Long id) {
        return cultivoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("un cultivo", id));
    }
}
