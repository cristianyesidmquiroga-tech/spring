package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
        @NotBlank(message = "Escribe tu correo o documento")
        @Size(max = 100, message = "El identificador es demasiado largo")
        String identificador,

        @NotBlank(message = "Escribe tu contraseña")
        @Size(max = 72, message = "La contraseña es demasiado larga")
        String password
) {
}
