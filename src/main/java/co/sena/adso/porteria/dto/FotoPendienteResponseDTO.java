package co.sena.adso.porteria.dto;

import co.sena.adso.porteria.entity.Usuario;
import java.time.LocalDateTime;

public record FotoPendienteResponseDTO(Long usuarioId, String nombre, String cargo, String documento,
                                       LocalDateTime fechaSubida) {

    public static FotoPendienteResponseDTO fromEntity(Usuario u) {
        return new FotoPendienteResponseDTO(u.getId(), u.getNombre(), u.getCargo(), u.getDocumento(),
                u.getFotoFechaSubida());
    }
}
