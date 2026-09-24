package co.sena.adso.porteria.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VerificacionResponseDTO(
        boolean encontrado,
        String tipo,
        Long id,
        String nombre,
        String documento,
        String cargo,
        String rol,
        String estado,
        Boolean tieneFoto,
        Boolean fotoAprobada,
        Boolean carnetActivo,
        List<EquipoResponseDTO> equipos,
        String tiempoAdentro,
        Boolean tiempoExcedido
) {
    public static VerificacionResponseDTO noEncontrado() {
        return new VerificacionResponseDTO(false, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
    }
}
