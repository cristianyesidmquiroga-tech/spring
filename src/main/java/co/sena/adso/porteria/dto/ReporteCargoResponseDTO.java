package co.sena.adso.porteria.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReporteCargoResponseDTO(
        String cargo,
        List<Conteo> hoy,
        List<Conteo> ultimos7Dias,
        String analisis,
        List<PersonaAdentro> adentro
) {
    public record Conteo(String grupo, long total) {
    }

    public record PersonaAdentro(Long id, String nombre, String documento, String programa, String ficha,
                                 LocalDateTime horaIngreso) {
    }
}
