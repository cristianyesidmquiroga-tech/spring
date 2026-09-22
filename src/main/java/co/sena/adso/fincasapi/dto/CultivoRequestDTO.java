package co.sena.adso.fincasapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CultivoRequestDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre admite máximo 80 caracteres")
        String nombre,

        @NotBlank(message = "El tipo es obligatorio (permanente o transitorio)")
        @Size(max = 30, message = "El tipo admite máximo 30 caracteres")
        String tipo,

        @NotNull(message = "El ciclo en días es obligatorio")
        @Positive(message = "El ciclo en días debe ser mayor que cero")
        Integer cicloDias
) {
}
