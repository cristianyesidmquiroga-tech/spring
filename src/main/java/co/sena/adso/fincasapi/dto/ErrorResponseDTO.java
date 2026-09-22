package co.sena.adso.fincasapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponseDTO(
        int status,
        String error,
        String mensaje,
        String ruta,
        OffsetDateTime fecha,
        List<CampoInvalido> campos
) {
    public record CampoInvalido(String campo, String mensaje) {
    }
}
