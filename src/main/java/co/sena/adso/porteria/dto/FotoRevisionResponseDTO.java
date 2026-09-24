package co.sena.adso.porteria.dto;

import co.sena.adso.porteria.entity.Usuario;
import java.time.LocalDateTime;

// Sin correo: la tarjeta de revisión solo necesita lo que permite reconocer a la persona
public record FotoRevisionResponseDTO(Long usuarioId, String nombre, String cargo, String documento, String ficha,
                                      String programa, String estado, String motivo, LocalDateTime fechaSubida) {

    public static FotoRevisionResponseDTO fromEntity(Usuario u) {
        return new FotoRevisionResponseDTO(u.getId(), u.getNombre(), u.getCargo(), u.getDocumento(), u.numeroFicha(),
                u.programaCarnet(), u.getFotoEstado(), u.getFotoMotivo(), u.getFotoFechaSubida());
    }
}
