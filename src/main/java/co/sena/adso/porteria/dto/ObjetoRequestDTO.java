package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ObjetoRequestDTO(
        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 150, message = "La descripción admite máximo 150 caracteres")
        String descripcion,

        // Opcional: si no lo trae se genera uno
        @Pattern(regexp = "^[A-Za-z0-9\\-_./ ]{0,60}$", message = "El serial solo admite letras, números y - _ . /")
        String serial,

        @Size(max = 100, message = "El propietario admite máximo 100 caracteres")
        String propietario,

        @Size(max = 255, message = "El motivo admite máximo 255 caracteres")
        String motivo
) {
}
