package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TextoMensajeRequestDTO(
        @NotBlank(message = "Escribe un mensaje antes de enviarlo.")
        @Size(max = 2000, message = "El mensaje no puede superar los 2000 caracteres.")
        String texto
) {
}
