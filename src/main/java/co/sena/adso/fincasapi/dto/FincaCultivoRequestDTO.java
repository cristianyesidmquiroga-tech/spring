package co.sena.adso.fincasapi.dto;

import co.sena.adso.fincasapi.enums.EstadoSiembra;
import co.sena.adso.fincasapi.enums.Temporada;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record FincaCultivoRequestDTO(
        @NotNull(message = "La finca es obligatoria")
        Long fincaId,

        @NotNull(message = "El cultivo es obligatorio")
        Long cultivoId,

        @NotNull(message = "El área sembrada es obligatoria")
        @Positive(message = "El área sembrada debe ser mayor que cero")
        Double areaSembradaHa,

        @NotNull(message = "La fecha de siembra es obligatoria")
        @PastOrPresent(message = "La fecha de siembra no puede estar en el futuro")
        LocalDate fechaSiembra,

        @NotNull(message = "La temporada es obligatoria")
        Temporada temporada,

        @NotNull(message = "El estado es obligatorio")
        EstadoSiembra estado
) {
}
