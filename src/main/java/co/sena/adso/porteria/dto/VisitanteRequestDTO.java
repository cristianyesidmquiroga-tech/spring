package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VisitanteRequestDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres")
        String nombre,

        @NotBlank(message = "El documento es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9.\\- ]{5,20}$", message = "El documento debe tener entre 5 y 20 letras o números")
        String documento,

        @Size(max = 255, message = "El motivo admite máximo 255 caracteres")
        String motivo
) {
}
