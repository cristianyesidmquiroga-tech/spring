package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MovimientoRequestDTO(
        @NotBlank(message = "Indica el tipo de entidad")
        @Pattern(regexp = "Usuario|Visitante|Vehiculo|ObjetoExterno", message = "Tipo de entidad no válido")
        String tipoEntidad,

        @NotNull(message = "Indica la entidad")
        Long entidadId,

        @NotBlank(message = "Indica si es entrada o salida")
        @Pattern(regexp = "Entrada|Salida", message = "El movimiento debe ser Entrada o Salida")
        String tipo,

        @Size(max = 5, message = "Máximo 5 equipos por movimiento")
        List<Long> equiposIds
) {
}
