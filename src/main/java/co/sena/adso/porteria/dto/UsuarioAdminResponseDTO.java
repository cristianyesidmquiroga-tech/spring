package co.sena.adso.porteria.dto;

import co.sena.adso.porteria.entity.Usuario;
import java.time.LocalDateTime;

public record UsuarioAdminResponseDTO(
        Long id,
        String nombre,
        String correo,
        Long rolId,
        String rol,
        String cargo,
        String tipoDocumento,
        String documento,
        String ficha,
        String programa,
        String horario,
        boolean perfilCompleto,
        boolean correoVerificado,
        String fotoEstado,
        boolean bloqueado,
        LocalDateTime bloqueadoHasta
) {
    public static UsuarioAdminResponseDTO fromEntity(Usuario u, LocalDateTime ahora) {
        return new UsuarioAdminResponseDTO(u.getId(), u.getNombre(), u.getCorreo(), u.getRol().getId(),
                u.getRol().getNombre(), u.getCargo(), u.getTipoDocumento(), u.getDocumento(), u.numeroFicha(),
                u.programaCarnet(), u.getHorario(), u.isPerfilCompleto(), u.isCorreoVerificado(), u.getFotoEstado(),
                u.estaBloqueado(ahora), u.getBloqueadoHasta());
    }
}
