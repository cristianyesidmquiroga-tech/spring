package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactoRequestDTO(
        @Size(max = 80, message = "Asunto inválido")
        String asunto,

        @NotBlank(message = "Escribe un mensaje antes de enviarlo.")
        @Size(max = 2000, message = "El mensaje no puede superar los 2000 caracteres.")
        String detalle
) {
}
