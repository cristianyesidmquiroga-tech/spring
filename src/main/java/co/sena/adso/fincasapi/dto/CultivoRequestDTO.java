package co.sena.adso.fincasapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CultivoRequestDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre admite máximo 80 caracteres")
        String nombre,

        @NotBlank(message = "El tipo es obligatorio")
        @Pattern(regexp = "permanente|transitorio", message = "El tipo debe ser permanente o transitorio")
        String tipo,

        @NotNull(message = "El ciclo en días es obligatorio")
        @Positive(message = "El ciclo en días debe ser mayor que cero")
        Integer cicloDias
) {
}
