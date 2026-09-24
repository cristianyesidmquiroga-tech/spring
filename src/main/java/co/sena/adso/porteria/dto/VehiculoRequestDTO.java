package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VehiculoRequestDTO(
        @NotBlank(message = "La placa es obligatoria")
        @Pattern(regexp = "^[A-Za-z0-9\\- ]{5,8}$", message = "La placa debe tener entre 5 y 8 letras o números")
        String placa,

        @NotBlank(message = "Indica si el vehículo es del SENA o externo")
        @Pattern(regexp = "SENA|Externo", message = "El tipo debe ser SENA o Externo")
        String tipo,

        @Size(max = 100, message = "El propietario admite máximo 100 caracteres")
        String propietario,

        @Size(max = 255, message = "El motivo admite máximo 255 caracteres")
        String motivo
) {
}
