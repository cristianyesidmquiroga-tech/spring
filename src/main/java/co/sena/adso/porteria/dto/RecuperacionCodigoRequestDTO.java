package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecuperacionCodigoRequestDTO(
        @NotBlank(message = "Escribe tu correo")
        @Size(max = 100, message = "El correo admite máximo 100 caracteres")
        String correo,

        @NotBlank(message = "Escribe el código")
        @Size(max = 10, message = "Código inválido")
        String codigo
) {
}
