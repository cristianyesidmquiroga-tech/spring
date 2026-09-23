package co.sena.adso.porteria.dto;

public record CarnetResponseDTO(
        boolean activo,
        String perfil,
        String nombres,
        String apellidos,
        String documento,
        String tipoSangre,
        String ficha,
        String programa,
        String fechaFinalizacion,
        String codigoBarras
) {
}
