package co.sena.adso.porteria.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DetalleAmbienteResponseDTO(String ficha, String programa, Instructor instructor, List<Aprendiz> aprendices) {

    public record Instructor(Long id, String nombre, String cargo, String programa, boolean tieneFoto) {
    }

    // presente queda en null mientras el instructor no tome la lista
    public record Aprendiz(Long id, String nombre, String documento, String cargo, boolean tieneFoto,
                           LocalDateTime llegada, Boolean presente, String evaluacion) {
    }
}
