package co.sena.adso.fincasapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FincaRequestDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres")
        String nombre,

        @NotBlank(message = "El propietario es obligatorio")
        @Size(max = 100, message = "El propietario admite máximo 100 caracteres")
        String propietario,

        @NotBlank(message = "La vereda es obligatoria")
        @Size(max = 100, message = "La vereda admite máximo 100 caracteres")
        String vereda,

        @NotBlank(message = "El municipio es obligatorio")
        @Size(max = 100, message = "El municipio admite máximo 100 caracteres")
        String municipio,

        @NotNull(message = "Las hectáreas son obligatorias")
        @Positive(message = "Las hectáreas deben ser mayores que cero")
        Double hectareas
) {
}
