package co.sena.adso.porteria.dto;

public record SesionResponseDTO(String token, long expiraEnMs, UsuarioSesionDTO usuario) {
}
