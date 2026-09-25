package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecuperacionCambioRequestDTO(
        @NotBlank(message = "Escribe tu correo")
        @Size(max = 100, message = "El correo admite máximo 100 caracteres")
        String correo,

        @Size(max = 64, message = "Permiso inválido")
        String permiso,

        @Size(max = 72, message = "La contraseña es demasiado larga")
        String password,

        @Size(max = 72, message = "La contraseña es demasiado larga")
        String confirmacion
) {
}
