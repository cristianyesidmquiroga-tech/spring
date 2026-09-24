package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ComunicadoRequestDTO(
        @NotBlank(message = "Elige el tipo de aviso")
        @Size(max = 40, message = "Tipo de aviso inválido")
        String tipo,

        @Size(max = 2000, message = "El mensaje admite máximo 2000 caracteres")
        String mensaje,

        @NotEmpty(message = "No se seleccionaron destinatarios")
        @Size(max = 500, message = "Máximo 500 destinatarios por envío")
        List<Long> destinatarios
) {
}
