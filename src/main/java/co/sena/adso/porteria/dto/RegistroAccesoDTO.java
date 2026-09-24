package co.sena.adso.porteria.dto;

import java.time.LocalDateTime;

/** Una fila del historial del panel, con los datos de la entidad ya resueltos. */
public record RegistroAccesoDTO(
        Long id,
        String tipo,
        LocalDateTime fecha,
        String tipoReferencia,
        Long referenciaId,
        String nombre,
        String documento,
        String clase,
        String detalle,
        boolean tieneFoto
) {
}
