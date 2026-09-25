package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CodigoRequestDTO(
        @NotBlank(message = "Escribe el código")
        @Size(max = 10, message = "Código inválido")
        String codigo
) {
}
