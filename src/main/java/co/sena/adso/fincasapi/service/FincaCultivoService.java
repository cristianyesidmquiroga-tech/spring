package co.sena.adso.fincasapi.service;

import co.sena.adso.fincasapi.dto.FincaCultivoRequestDTO;
import co.sena.adso.fincasapi.dto.FincaCultivoResponseDTO;
import co.sena.adso.fincasapi.entity.Cultivo;
import co.sena.adso.fincasapi.entity.Finca;
import co.sena.adso.fincasapi.entity.FincaCultivo;
import co.sena.adso.fincasapi.enums.EstadoSiembra;
import co.sena.adso.fincasapi.exception.ResourceNotFoundException;
import co.sena.adso.fincasapi.exception.BusinessException;
import co.sena.adso.fincasapi.repository.FincaCultivoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FincaCultivoService {

    private static final long SIN_EXCLUIR = -1L;

    private final FincaCultivoRepository siembraRepository;
    private final FincaService fincaService;
    private final CultivoService cultivoService;

    public FincaCultivoService(FincaCultivoRepository siembraRepository,
                               FincaService fincaService,
                               CultivoService cultivoService) {
        this.siembraRepository = siembraRepository;
        this.fincaService = fincaService;
        this.cultivoService = cultivoService;
    }

    @Transactional(readOnly = true)
    public List<FincaCultivoResponseDTO> listar() {
        return siembraRepository.listarConDetalle().stream()
                .map(FincaCultivoResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FincaCultivoResponseDTO> listarPorFinca(Long fincaId) {
        fincaService.buscarFinca(fincaId);
        return siembraRepository.listarPorFinca(fincaId).stream()
                .map(FincaCultivoResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public FincaCultivoResponseDTO obtener(Long id) {
        return FincaCultivoResponseDTO.fromEntity(buscarSiembra(id));
    }

    @Transactional
    public FincaCultivoResponseDTO crear(FincaCultivoRequestDTO datos) {
        Finca finca = fincaService.buscarFinca(datos.fincaId());
        Cultivo cultivo = cultivoService.buscarCultivo(datos.cultivoId());
        validarArea(finca, datos, SIN_EXCLUIR);

        FincaCultivo siembra = new FincaCultivo(finca, cultivo, datos.areaSembradaHa(),
                datos.fechaSiembra(), datos.temporada(), datos.estado());
        return FincaCultivoResponseDTO.fromEntity(siembraRepository.save(siembra));
    }

    @Transactional
    public FincaCultivoResponseDTO actualizar(Long id, FincaCultivoRequestDTO datos) {
        FincaCultivo siembra = buscarSiembra(id);
        Finca finca = fincaService.buscarFinca(datos.fincaId());
        Cultivo cultivo = cultivoService.buscarCultivo(datos.cultivoId());
        validarArea(finca, datos, id);

        siembra.actualizar(finca, cultivo, datos.areaSembradaHa(),
                datos.fechaSiembra(), datos.temporada(), datos.estado());
        return FincaCultivoResponseDTO.fromEntity(siembra);
    }

    @Transactional
    public void eliminar(Long id) {
        siembraRepository.delete(buscarSiembra(id));
    }

    /**
     * Lo sembrado en cultivos activos no puede pasar del tamaño de la finca.
     * Las siembras cosechadas o inactivas ya liberaron el terreno, por eso no suman.
     */
    private void validarArea(Finca finca, FincaCultivoRequestDTO datos, long siembraExcluida) {
        if (datos.areaSembradaHa() > finca.getHectareas()) {
            throw new BusinessException("El área sembrada (" + datos.areaSembradaHa()
                    + " ha) es mayor que la finca " + finca.getNombre() + " (" + finca.getHectareas() + " ha)");
        }
        if (datos.estado() != EstadoSiembra.ACTIVO) {
            return;
        }
        double ocupada = siembraRepository.sumarArea(finca.getId(), EstadoSiembra.ACTIVO, siembraExcluida);
        double disponible = finca.getHectareas() - ocupada;
        if (datos.areaSembradaHa() > disponible) {
            throw new BusinessException("En la finca " + finca.getNombre() + " quedan " + disponible
                    + " ha libres y se intentan sembrar " + datos.areaSembradaHa() + " ha");
        }
    }

    private FincaCultivo buscarSiembra(Long id) {
        return siembraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("una siembra", id));
    }
}
