package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambioContrasenaRequestDTO(
        @NotBlank(message = "Escribe tu contraseña actual")
        @Size(max = 72, message = "La contraseña es demasiado larga")
        String actual,

        @NotBlank(message = "Escribe la nueva contraseña")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String nueva,

        @NotBlank(message = "Confirma la nueva contraseña")
        @Size(max = 72, message = "La contraseña es demasiado larga")
        String confirmacion
) {
}
