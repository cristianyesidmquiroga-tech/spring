package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.CatalogosResponseDTO;
import co.sena.adso.porteria.dto.CatalogosResponseDTO.FichaOpcion;
import co.sena.adso.porteria.dto.CatalogosResponseDTO.Opcion;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.repository.FichaRepository;
import co.sena.adso.porteria.repository.RolRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogoService {

    private final RolRepository rolRepository;
    private final FichaRepository fichaRepository;
    private final DocumentoService documentoService;
    private final AuthService authService;

    public CatalogoService(RolRepository rolRepository, FichaRepository fichaRepository,
                           DocumentoService documentoService, AuthService authService) {
        this.rolRepository = rolRepository;
        this.fichaRepository = fichaRepository;
        this.documentoService = documentoService;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public CatalogosResponseDTO obtener() {
        Usuario usuario = authService.usuarioActual();
        Long fichaActual = usuario.getFichaRef() != null ? usuario.getFichaRef().getId() : -1L;
        return new CatalogosResponseDTO(
                rolRepository.findAll(Sort.by("id")).stream().map(r -> new Opcion(r.getId(), r.getNombre())).toList(),
                Usuario.CARGOS_VALIDOS,
                documentoService.catalogo(),
                Usuario.TIPOS_SANGRE,
                fichaRepository.listarParaSelector(fichaActual).stream()
                        .map(f -> new FichaOpcion(f.getId(), f.getNumero(), f.getPrograma())).toList());
    }
}
