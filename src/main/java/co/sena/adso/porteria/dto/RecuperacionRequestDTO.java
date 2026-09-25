package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecuperacionRequestDTO(
        @NotBlank(message = "Escribe tu correo")
        @Size(max = 100, message = "El correo admite máximo 100 caracteres")
        String correo,

        @Size(max = 4000, message = "Verificación inválida")
        String captcha
) {
}
