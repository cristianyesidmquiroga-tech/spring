package co.sena.adso.porteria.dto;

import co.sena.adso.porteria.entity.Usuario;

public record UsuarioSesionDTO(
        Long id,
        String nombre,
        String correo,
        String rol,
        String cargo,
        boolean debeCambiarContrasena,
        boolean correoVerificado,
        boolean perfilCompleto,
        String fotoEstado,
        boolean tutorialVisto,
        Permisos permisos
) {
    public record Permisos(boolean admin, boolean operarPorteria, boolean asesorar,
                           boolean gestionarAsistencia, boolean registrarEquipos, boolean verAmbientes) {
    }

    public static UsuarioSesionDTO fromEntity(Usuario u) {
        return new UsuarioSesionDTO(u.getId(), u.getNombre(), u.getCorreo(), u.getRol().getNombre(), u.getCargo(),
                u.isDebeCambiarContrasena(), u.isCorreoVerificado(), u.isPerfilCompleto(), u.getFotoEstado(),
                u.isTutorialVisto(),
                new Permisos(u.esAdmin(), u.puedeOperarPorteria(), u.puedeAsesorar(),
                        u.puedeGestionarAsistencia(), u.puedeRegistrarEquipos(), u.puedeVerAmbientes()));
    }
}
