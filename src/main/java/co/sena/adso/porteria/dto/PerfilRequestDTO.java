package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PerfilRequestDTO(
        @Size(max = 100, message = "Los nombres admiten máximo 100 caracteres")
        @Pattern(regexp = "^[\\p{L} '\\-]*$", message = "Los nombres solo admiten letras, espacios, apóstrofos y guiones")
        String nombres,

        @Size(max = 100, message = "Los apellidos admiten máximo 100 caracteres")
        @Pattern(regexp = "^[\\p{L} '\\-]*$", message = "Los apellidos solo admiten letras, espacios, apóstrofos y guiones")
        String apellidos,

        @Size(max = 5, message = "Tipo de documento inválido")
        String tipoDocumento,

        @Size(max = 30, message = "El documento es demasiado largo")
        String documento,

        @Size(max = 100, message = "El programa admite máximo 100 caracteres")
        @Pattern(regexp = "^[\\p{L} ]*$", message = "El programa o área solo admite letras y espacios")
        String programa,

        Long fichaId,

        @Size(max = 5, message = "Tipo de sangre inválido")
        String tipoSangre
) {
}
