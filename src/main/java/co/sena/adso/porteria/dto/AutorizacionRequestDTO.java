package co.sena.adso.porteria.dto;

import jakarta.validation.constraints.Size;

/** Quién autorizó y por qué; queda en la auditoría de la acción. */
public record AutorizacionRequestDTO(
        @Size(max = 100, message = "Autorizado por admite máximo 100 caracteres")
        String autorizadoPor,

        @Size(max = 500, message = "El motivo admite máximo 500 caracteres")
        String motivo
) {
}
