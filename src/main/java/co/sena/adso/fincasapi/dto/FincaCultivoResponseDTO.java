package co.sena.adso.fincasapi.dto;

import co.sena.adso.fincasapi.entity.FincaCultivo;
import co.sena.adso.fincasapi.enums.EstadoSiembra;
import co.sena.adso.fincasapi.enums.Temporada;
import java.time.LocalDate;

public record FincaCultivoResponseDTO(
        Long id,
        Long fincaId,
        String finca,
        Long cultivoId,
        String cultivo,
        Double areaSembradaHa,
        LocalDate fechaSiembra,
        Temporada temporada,
        EstadoSiembra estado
) {
    public static FincaCultivoResponseDTO fromEntity(FincaCultivo fc) {
        return new FincaCultivoResponseDTO(fc.getId(),
                fc.getFinca().getId(), fc.getFinca().getNombre(),
                fc.getCultivo().getId(), fc.getCultivo().getNombre(),
                fc.getAreaSembradaHa(), fc.getFechaSiembra(), fc.getTemporada(), fc.getEstado());
    }
}
