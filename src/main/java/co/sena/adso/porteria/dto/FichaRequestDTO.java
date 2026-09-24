package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record FichaRequestDTO(
        @NotBlank(message = "El número de ficha es obligatorio")
        @Pattern(regexp = "^\\d{4,12}$", message = "El número de ficha debe tener entre 4 y 12 dígitos")
        String numero,

        @NotBlank(message = "El nombre del programa de formación es obligatorio")
        @Size(max = 150, message = "El nombre del programa no puede superar los 150 caracteres")
        String programa,

        LocalDate fechaFinalizacion
) {
}
