package co.sena.adso.porteria.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record FotosRevisionResponseDTO(List<FotoRevisionResponseDTO> content, int number, int totalPages,
                                       long totalElements, boolean first, boolean last, String estado,
                                       long pendientes) {

    public static FotosRevisionResponseDTO de(Page<FotoRevisionResponseDTO> pagina, String estado, long pendientes) {
        return new FotosRevisionResponseDTO(pagina.getContent(), pagina.getNumber(), pagina.getTotalPages(),
                pagina.getTotalElements(), pagina.isFirst(), pagina.isLast(), estado, pendientes);
    }
}
