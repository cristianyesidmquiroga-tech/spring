package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record IncidenteRequestDTO(
        @Pattern(regexp = "Usuario|Visitante|Vehiculo|ObjetoExterno|Desconocido", message = "Tipo de entidad no válido")
        String tipoEntidad,

        Long entidadId,

        @NotBlank(message = "Describe el incidente")
        @Size(max = 1000, message = "El detalle admite máximo 1000 caracteres")
        String detalles
) {
}
