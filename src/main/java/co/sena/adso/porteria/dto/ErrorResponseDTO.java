package co.sena.adso.porteria.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDTO(
        int status,
        String error,
        String mensaje,
        String ruta,
        OffsetDateTime fecha,
        List<CampoInvalido> campos,
        Long bloqueadoSegundos
) {
    public record CampoInvalido(String campo, String mensaje) {
    }
}
