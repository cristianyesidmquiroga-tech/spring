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
        String codigoBarras,
        String codigoBarrasSvg,
        String regional,
        String centro,
        String aseguradora,
        String aseguradoraTel,
        String poliza
) {
}
