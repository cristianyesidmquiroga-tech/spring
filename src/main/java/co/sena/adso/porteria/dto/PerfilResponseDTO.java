package co.sena.adso.porteria.dto;

import co.sena.adso.porteria.entity.Usuario;

public record PerfilResponseDTO(
        Long id,
        String nombre,
        String nombres,
        String apellidos,
        String correo,
        String rol,
        String cargo,
        String tipoDocumento,
        String documento,
        String programa,
        Long fichaId,
        String ficha,
        String tipoSangre,
        boolean perfilCompleto,
        boolean tieneFoto,
        String fotoEstado,
        String fotoMotivo
) {
    public static PerfilResponseDTO fromEntity(Usuario u) {
        return new PerfilResponseDTO(u.getId(), u.getNombre(), u.getNombres(), u.getApellidos(), u.getCorreo(),
                u.getRol().getNombre(), u.getCargo(), u.getTipoDocumento(), u.getDocumento(), u.programaCarnet(),
                u.getFichaRef() != null ? u.getFichaRef().getId() : null, u.numeroFicha(), u.getTipoSangre(),
                u.isPerfilCompleto(), u.tieneFotoPropia(), u.getFotoEstado(), u.getFotoMotivo());
    }
}
