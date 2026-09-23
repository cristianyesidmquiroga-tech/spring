package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RevisionFotoRequestDTO(
        @NotNull(message = "Indica si la foto se aprueba o se rechaza")
        Boolean aprobada,

        @Size(max = 500, message = "El motivo admite máximo 500 caracteres")
        String motivo
) {
}
