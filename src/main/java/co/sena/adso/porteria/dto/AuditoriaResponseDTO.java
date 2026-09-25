package co.sena.adso.porteria.dto;

import co.sena.adso.porteria.entity.Auditoria;
import java.time.LocalDateTime;

public record AuditoriaResponseDTO(Long id, LocalDateTime fecha, Long usuarioId, String nombreUsuario, String accion,
                                   String autorizadoPor, String motivo, String detalles) {

    public static AuditoriaResponseDTO fromEntity(Auditoria a) {
        return new AuditoriaResponseDTO(a.getId(), a.getFecha(), a.getUsuarioId(), a.getNombreUsuario(), a.getAccion(),
                a.getAutorizadoPor(), a.getMotivo(), a.getDetalles());
    }
}
