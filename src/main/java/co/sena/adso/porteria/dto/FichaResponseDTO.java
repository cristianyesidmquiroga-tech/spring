package co.sena.adso.porteria.dto;

import co.sena.adso.porteria.entity.Ficha;
import java.time.LocalDate;

public record FichaResponseDTO(
        Long id,
        String numero,
        String programa,
        LocalDate fechaFinalizacion,
        boolean activa,
        long aprendices
) {
    public static FichaResponseDTO fromEntity(Ficha f, long aprendices) {
        return new FichaResponseDTO(f.getId(), f.getNumero(), f.getPrograma(), f.getFechaFinalizacion(), f.isActiva(),
                aprendices);
    }
}
