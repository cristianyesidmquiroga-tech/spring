package co.sena.adso.fincasapi.service;

import co.sena.adso.fincasapi.dto.FincaRequestDTO;
import co.sena.adso.fincasapi.dto.FincaResponseDTO;
import co.sena.adso.fincasapi.entity.Finca;
import co.sena.adso.fincasapi.enums.EstadoSiembra;
import co.sena.adso.fincasapi.exception.ResourceNotFoundException;
import co.sena.adso.fincasapi.exception.BusinessException;
import co.sena.adso.fincasapi.repository.FincaCultivoRepository;
import co.sena.adso.fincasapi.repository.FincaRepository;
import co.sena.adso.fincasapi.specification.FincaSpecification;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FincaService {

    private final FincaRepository fincaRepository;
    private final FincaCultivoRepository siembraRepository;

    public FincaService(FincaRepository fincaRepository, FincaCultivoRepository siembraRepository) {
        this.fincaRepository = fincaRepository;
        this.siembraRepository = siembraRepository;
    }

    @Transactional(readOnly = true)
    public List<FincaResponseDTO> listar() {
        return fincaRepository.findAll(Sort.by("id")).stream()
                .map(FincaResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<FincaResponseDTO> listarPaginado(Pageable pageable) {
        return fincaRepository.findAll(pageable).map(FincaResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<FincaResponseDTO> buscar(String municipio, String propietario, Double hectareasMin) {
        return fincaRepository.findAll(FincaSpecification.filtrar(municipio, propietario, hectareasMin), Sort.by("nombre"))
                .stream()
                .map(FincaResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public FincaResponseDTO obtener(Long id) {
        return FincaResponseDTO.fromEntity(buscarFinca(id));
    }

    @Transactional
    public FincaResponseDTO crear(FincaRequestDTO datos) {
        Finca finca = new Finca(datos.nombre().trim(), datos.propietario().trim(), datos.vereda().trim(),
                datos.municipio().trim(), datos.hectareas());
        return FincaResponseDTO.fromEntity(fincaRepository.save(finca));
    }

    @Transactional
    public FincaResponseDTO actualizar(Long id, FincaRequestDTO datos) {
        Finca finca = buscarFinca(id);
        double areaActiva = siembraRepository.sumarArea(id, EstadoSiembra.ACTIVO, -1L);
        if (datos.hectareas() < areaActiva) {
            throw new BusinessException("La finca tiene " + areaActiva
                    + " ha sembradas en cultivos activos; no puede quedar con " + datos.hectareas() + " ha");
        }
        finca.actualizar(datos.nombre().trim(), datos.propietario().trim(), datos.vereda().trim(),
                datos.municipio().trim(), datos.hectareas());
        return FincaResponseDTO.fromEntity(finca);
    }

    @Transactional
    public void eliminar(Long id) {
        fincaRepository.delete(buscarFinca(id));
    }

    Finca buscarFinca(Long id) {
        return fincaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("una finca", id));
    }
}
