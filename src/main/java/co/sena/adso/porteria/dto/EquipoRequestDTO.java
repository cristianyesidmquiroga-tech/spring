package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipoRequestDTO(
        @NotBlank(message = "El nombre del equipo es obligatorio")
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres")
        String nombre,

        @Size(max = 60, message = "El serial admite máximo 60 caracteres")
        String serial,

        @NotBlank(message = "El tipo de equipo es obligatorio")
        @Size(max = 20, message = "Tipo de equipo inválido")
        String tipo
) {
}
