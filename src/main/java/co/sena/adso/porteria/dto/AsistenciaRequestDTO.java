package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AsistenciaRequestDTO(
        @NotBlank(message = "Escribe el número de ficha")
        @Size(max = 20, message = "El número de ficha es demasiado largo")
        String ficha,

        @NotNull(message = "Falta la lista de presentes")
        @Size(max = 500, message = "La lista de presentes es demasiado larga")
        List<Long> presentes
) {
}
